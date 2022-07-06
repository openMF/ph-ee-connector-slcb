package org.mifos.connector.slcb.camel.routes.transfer;

import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.slcb.dto.BalanceRequestDTO;
import org.mifos.connector.slcb.dto.PaymentRequestDTO;
import org.mifos.connector.slcb.dto.ReconciliationRequestDTO;
import org.springframework.stereotype.Component;

import static org.mifos.connector.slcb.camel.config.CamelProperties.*;

@Component
public class ReconciliationRoutes extends BaseSLCBRouteBuilder {

    private final
    ZeebeCompleteCommandPublisher zeebeCompleteCommandPublisher;

    public ReconciliationRoutes(ZeebeCompleteCommandPublisher zeebeCompleteCommandPublisher) {
        this.zeebeCompleteCommandPublisher = zeebeCompleteCommandPublisher;
    }

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
                .to("direct:reconciliation-response-handler")
                .process(zeebeCompleteCommandPublisher);


        /*
         * Calls SLCB reconciliation API
         */
        getBaseAuthDefinitionBuilder("direct:initiate-reconciliation", HttpRequestMethod.GET)
                .process(exchange -> {
                    ReconciliationRequestDTO reconciliationRequestDTO = objectMapper.readValue(
                            exchange.getProperty(SLCB_RECONCILIATION_REQUEST, String.class),
                            ReconciliationRequestDTO.class);

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
                .unmarshal().json(JsonLibrary.Jackson, BalanceRequestDTO.class)
                .process(exchange -> {
                    PaymentRequestDTO response = exchange.getIn().getBody(PaymentRequestDTO.class);
                    logger.info("Status: " + response.getStatus());
                    exchange.setProperty("BODY", response);
                })
                .otherwise()
                .log(LoggingLevel.ERROR, "Balance request unsuccessful")
                .process(exchange -> exchange.setProperty("BODY", "Fetch balance request unsuccessful"))
                .setBody(exchange -> exchange.getProperty("BODY"));
    }

}
