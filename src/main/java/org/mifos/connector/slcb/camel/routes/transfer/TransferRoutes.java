package org.mifos.connector.slcb.camel.routes.transfer;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.slcb.dto.*;
import org.mifos.connector.slcb.utils.TransactionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.mifos.connector.slcb.camel.config.CamelProperties.*;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.*;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.TRANSFER_FAILED;

@Component
public class TransferRoutes extends BaseSLCBRouteBuilder {

    @Override
    public void configure() {

        /*
         * Base route for transactions
         */
        from("direct:transfer-route")
                .id("transfer-route")
                .log(LoggingLevel.INFO, "Transfer route started")
                .to("direct:get-access-token")
                .log(LoggingLevel.INFO, "Got access token, moving on")
                .log(LoggingLevel.INFO, "Moving on to API call")
                .to("direct:commit-transaction")
                .log(LoggingLevel.INFO, "Status: ${header.CamelHttpResponseCode}")
                .log(LoggingLevel.INFO, "Transaction API response: ${body}")
                .to("direct:transaction-response-handler");

        /*
         * Route to handle async API responses
         */
        from("direct:transaction-response-handler")
                .id("transaction-response-handler")
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .log(LoggingLevel.INFO, "Transaction request successful")
                .unmarshal().json(JsonLibrary.Jackson, PaymentRequestDTO.class)
                .process((exchange) -> {
                    PaymentRequestDTO paymentRequestDTO = exchange.getIn().getBody(PaymentRequestDTO.class);
                    exchange.setProperty(SLCB_TRANSACTION_RESPONSE, paymentRequestDTO);
                    logger.info("Status: " + paymentRequestDTO.getStatus());
                }
                )
                .to("direct:update-status")
                // setting localfilpath as result file to make sure result file is uploaded
                .setProperty(LOCAL_FILE_PATH, exchangeProperty(RESULT_FILE))
                .to("direct:update-result-file")
                .to("direct:upload-file")
                .setProperty(TRANSFER_FAILED, constant(false))
                .otherwise()
                .log(LoggingLevel.ERROR, "Transaction request unsuccessful")
                .process(exchange -> {
                    exchange.setProperty(TRANSACTION_ID, exchange.getProperty(CORRELATION_ID)); // TODO: Improve this
                    exchange.setProperty(ERROR_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                    exchange.setProperty(ERROR_DESCRIPTION, exchange.getIn().getBody(String.class));

                    exchange.setProperty(ONGOING_TRANSACTION, 0);
                    exchange.setProperty(FAILED_TRANSACTION, exchange.getProperty(TOTAL_TRANSACTION));
                    exchange.setProperty(COMPLETED_TRANSACTION, 0);

                    exchange.setProperty(ONGOING_AMOUNT, 0);
                    exchange.setProperty(FAILED_AMOUNT, exchange.getProperty(FAILED_AMOUNT));
                    exchange.setProperty(COMPLETED_AMOUNT, 0);
                })
                .to("direct:delete-local-file")
                .setProperty(TRANSFER_FAILED, constant(true));

        /*
         * Calls SLCB API to commit transaction
         */
        getBaseAuthDefinitionBuilder("direct:commit-transaction", HttpRequestMethod.POST)
                .setBody(exchange -> exchange.getProperty(SLCB_CHANNEL_REQUEST))
                .marshal().json(JsonLibrary.Jackson)
                .log(LoggingLevel.INFO, "Transaction Request Body: ${body}")
                .toD(slcbConfig.transactionRequestUrl + "?bridgeEndpoint=true&throwExceptionOnFailure=false");

        from("direct:update-status")
                .id("direct:update-status")
                .log("Starting route direct:update-status")
                .process(exchange -> {
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    List<TransactionResult> transactionResults = new ArrayList<>();
                    PaymentRequestDTO paymentRequestDTO = exchange.getProperty(SLCB_TRANSACTION_RESPONSE,
                            PaymentRequestDTO.class);
                    Status status = paymentRequestDTO.getStatus();
                    exchange.setProperty(STATUS_CODE, status.code);
                    exchange.setProperty(STATUS_DESCRIPTION, status.getDescription());

                    Map<Integer, Integer> idIndexMap = new HashMap<>();
                    for (int i = 0; i < transactionList.size(); i++) {
                        idIndexMap.put(transactionList.get(i).getId(), i);
                    }

                    int failed = 0;
                    int ongoing = 0;
                    int completed = 0;

                    long ongoingAmount = 0L;
                    long failedAmount = 0L;
                    long completedAmount = 0L;

                    for (Payee payee: paymentRequestDTO.getPayees()) {
                        int index = idIndexMap.get(payee.getExternalTransactionId());
                        Transaction transaction = transactionList.get(index);
                        transaction.setPayerIdentifier(slcbConfig.sourceAccount);
                        TransactionResult transactionResult = TransactionUtils.mapToResultDTO(transaction);
                        if (payee.getStatus().getCode() == 0) {
                            transactionResult.setStatus("SUCCESS");

                            completed++;
                            completedAmount += payee.getAmount();
                        } else if (payee.getStatus().getCode() == 1) {
                            transactionResult.setStatus("PENDING");

                            ongoing++;
                            ongoingAmount += payee.getAmount();
                        } else {
                            transactionResult.setStatus("FAILED");
                            transactionResult.setErrorCode(String.format("%s", payee.getStatus().getCode()));
                            transactionResult.setErrorDescription(payee.getStatus().getDescription());

                            failed++;
                            failedAmount += payee.getAmount();
                        }

                        transactionResults.add(transactionResult);
                    }

                    logger.info("Failed: {}, Ongoing: {}, Completed: {}", failedAmount, ongoingAmount, completedAmount);

                    exchange.setProperty(RESULT_TRANSACTION_LIST, transactionResults);
                    exchange.setProperty(OVERRIDE_HEADER, true);
                    exchange.setProperty(ONGOING_TRANSACTION, ongoing);
                    exchange.setProperty(FAILED_TRANSACTION, failed);
                    exchange.setProperty(COMPLETED_TRANSACTION, completed);
                    exchange.setProperty(ONGOING_AMOUNT, ongoingAmount);
                    exchange.setProperty(FAILED_AMOUNT, failedAmount);
                    exchange.setProperty(COMPLETED_AMOUNT, completedAmount);
                });

    }
}
