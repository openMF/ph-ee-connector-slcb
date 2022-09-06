package org.mifos.connector.slcb.camel.routes.transfer;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.slcb.dto.Payee;
import org.mifos.connector.slcb.dto.PaymentRequestDTO;
import org.mifos.connector.slcb.dto.ReconciliationRequestDTO;
import org.mifos.connector.slcb.utils.SecurityUtils;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mifos.connector.slcb.camel.config.CamelProperties.*;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.*;

@Component
public class ReconciliationRoutes extends BaseSLCBRouteBuilder {

    @Override
    public void configure() {

        /*
         * Base route for reconciliation
         */
        from("direct:reconciliation-route")
                .id("direct:reconciliation-route")
                .log(LoggingLevel.INFO, "Transfer route started")
                .to("direct:get-access-token")
                .log(LoggingLevel.INFO, "Got access token, moving on")
                .log(LoggingLevel.INFO, "Moving on to API call")
                .to("direct:initiate-reconciliation")
                .log(LoggingLevel.INFO, "Status: ${header.CamelHttpResponseCode}")
                .log(LoggingLevel.INFO, "Fetch Balance API response: ${body}")
                .to("direct:reconciliation-response-handler");


        /*
         * Calls SLCB reconciliation API
         */
        getBaseAuthDefinitionBuilder("direct:initiate-reconciliation", HttpRequestMethod.GET)
                .setHeader(Exchange.CONTENT_TYPE, simple("application/x-www-form-urlencoded"))
                .process(exchange -> {
                    ReconciliationRequestDTO reconciliationRequestDTO = new ReconciliationRequestDTO();

                    SimpleDateFormat format = new SimpleDateFormat(slcbConfig.dateFormat);
                    Date dt = new Date();
                    String to = format.format(dt);
                    String from = format.format(new Date(System.currentTimeMillis()-120000));

                    String authCode = SecurityUtils.signContent(UUID.randomUUID().toString(), slcbConfig.signatureKey);
                    reconciliationRequestDTO.setAuthorizationCode(authCode);
                    reconciliationRequestDTO.setBban(slcbConfig.sourceAccount);
                    reconciliationRequestDTO.setTo(to);
                    reconciliationRequestDTO.setFrom(from);

                    exchange.setProperty(SLCB_RECONCILIATION_REQUEST, reconciliationRequestDTO);
                })
                .setBody(exchange -> exchange.getProperty(SLCB_RECONCILIATION_REQUEST))
                .log(LoggingLevel.INFO, "Reconciliation Request Body: ${body}")
                .toD(slcbConfig.reconciliationUrl + "?bridgeEndpoint=true&throwExceptionOnFailure=false");

        /*
         * Route to handle reconciliation async API responses
         */
        from("direct:reconciliation-response-handler")
                .id("balance-response-handler")
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .log(LoggingLevel.INFO, "Balance request successful")
                .unmarshal().json(JsonLibrary.Jackson, PaymentRequestDTO.class)
                .to("direct:download-file")
                .to("direct:get-transaction-array")
                .process(exchange -> {
                    PaymentRequestDTO response = exchange.getIn().getBody(PaymentRequestDTO.class);
                    logger.info("Status: " + response.getStatus());
                    exchange.setProperty(SLCB_TRANSACTION_RESPONSE, response);
                })
                .to("direct:update-status")
                .to("direct:update-file")
                .to("direct:upload-file")
                .to("direct:update-variable")
                .otherwise()
                .log(LoggingLevel.ERROR, "Reconciliation request unsuccessful")
                .process(exchange -> {
                    exchange.setProperty(RECONCILIATION_SUCCESS, false);
                    exchange.setProperty(ERROR_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                    exchange.setProperty(ERROR_DESCRIPTION, exchange.getIn().getBody(String.class));
                })
                .setProperty(RECONCILIATION_SUCCESS, constant(false))
                .process(exchange -> exchange.setProperty("BODY", "Fetch balance request unsuccessful"))
                .setBody(exchange -> exchange.getProperty("BODY"));

        from("direct:update-variable")
                .id("direct:update-variable")
                .process(exchange -> {
                    PaymentRequestDTO paymentRequestDTO = exchange.getProperty(SLCB_TRANSACTION_RESPONSE,
                            PaymentRequestDTO.class);

                    int failedTxn = 0;
                    int ongoingTxn = 0;
                    int completedTxn = 0;

                    long ongoingAmount = 0L;
                    long failedAmount = 0L;
                    long completedAmount = 0L;

                    for (Payee payee: paymentRequestDTO.getPayees()) {
                        if (payee.getStatus().getCode() == 0) {
                            // 0 is the SLCB success code
                            completedTxn++;
                            completedAmount += payee.getAmount();
                        } else if (payee.getStatus().getCode() == 1) {
                            ongoingTxn++;
                            ongoingAmount += payee.getAmount();
                        } else {
                            failedTxn++;
                            failedAmount += payee.getAmount();
                        }
                    }

                    exchange.setProperty(ONGOING_TRANSACTION, ongoingTxn);
                    exchange.setProperty(FAILED_TRANSACTION, failedTxn);
                    exchange.setProperty(COMPLETED_TRANSACTION, completedTxn);
                    exchange.setProperty(ONGOING_AMOUNT, ongoingAmount);
                    exchange.setProperty(FAILED_AMOUNT, failedAmount);
                    exchange.setProperty(COMPLETED_AMOUNT, completedAmount);

                    double percentage = paymentRequestDTO.getTotalAmountToBePaid() / paymentRequestDTO.getTotalAmountPaid();
                    percentage *= 100;
                    if (percentage >= 95) {
                        exchange.setProperty(RECONCILIATION_SUCCESS, true);
                    } else {
                        exchange.setProperty(RECONCILIATION_SUCCESS, false);
                        exchange.setProperty(ERROR_CODE, ""+paymentRequestDTO.getStatus().code);
                        exchange.setProperty(ERROR_DESCRIPTION, paymentRequestDTO.getStatus().description);
                    }
                });
    }

}
