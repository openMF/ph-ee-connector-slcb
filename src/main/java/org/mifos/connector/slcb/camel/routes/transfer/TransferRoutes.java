package org.mifos.connector.slcb.camel.routes.transfer;

import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.common.gsma.dto.GSMATransaction;
import org.mifos.connector.slcb.config.AwsFileTransferService;
import org.mifos.connector.slcb.dto.PaymentRequestDTO;
import org.mifos.connector.slcb.utils.CsvUtils;
import org.mifos.connector.slcb.utils.SLCBUtils;
import org.mifos.connector.slcb.utils.SecurityUtils;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.List;
import java.util.UUID;
import static org.mifos.connector.slcb.camel.config.CamelProperties.*;

@Component
public class TransferRoutes extends BaseSLCBRouteBuilder {

    private final TransferResponseProcessor transferResponseProcessor;

    private final AwsFileTransferService awsFileTransferService;

    private final
    ZeebeCompleteCommandPublisher zeebeCompleteCommandPublisher;

    public TransferRoutes(TransferResponseProcessor transferResponseProcessor, AwsFileTransferService awsFileTransferService, ZeebeCompleteCommandPublisher zeebeCompleteCommandPublisher) {
        this.transferResponseProcessor = transferResponseProcessor;
        this.awsFileTransferService = awsFileTransferService;
        this.zeebeCompleteCommandPublisher = zeebeCompleteCommandPublisher;
    }

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
                .to("direct:transaction-response-handler")
                .process(zeebeCompleteCommandPublisher);

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
                .to("direct:upload-to-s3")
                .otherwise()
                .log(LoggingLevel.ERROR, "Transaction request unsuccessful")
                .process(exchange -> {
                    exchange.setProperty(TRANSACTION_ID, exchange.getProperty(CORRELATION_ID)); // TODO: Improve this
                })
                .setProperty(TRANSACTION_FAILED, constant(true))
                .endChoice()
                .process(transferResponseProcessor);

        /*
         * Calls SLCB API to commit transaction
         */
        getBaseAuthDefinitionBuilder("direct:commit-transaction", HttpRequestMethod.POST)
                .process(exchange -> {
                    String signedBody = SecurityUtils.signContent(UUID.randomUUID().toString(), slcbConfig.signatureKey);
                    PaymentRequestDTO slcbChannelRequestBody = exchange.getProperty(SLCB_CHANNEL_REQUEST,
                            PaymentRequestDTO.class);
                    slcbChannelRequestBody.setAuthorizationCode(signedBody);
                    exchange.setProperty(SLCB_CHANNEL_REQUEST, slcbChannelRequestBody);
                })
                .setBody(exchange -> exchange.getProperty(SLCB_CHANNEL_REQUEST))
                .marshal().json(JsonLibrary.Jackson)
                .log(LoggingLevel.INFO, "Transaction Request Body: ${body}")
                .toD(slcbConfig.transactionRequestUrl + "?bridgeEndpoint=true&throwExceptionOnFailure=false");

        /*
         * Generates and uploads the CSV to AWS S3
         */
        from("direct:upload-to-s3")
                .id("upload-to-s3")
                .log(LoggingLevel.INFO, "Uploading to S3 route started.")
                .process(exchange -> {
                    PaymentRequestDTO paymentRequestDTO = exchange.getProperty(SLCB_TRANSACTION_RESPONSE,
                            PaymentRequestDTO.class);
                    List<GSMATransaction> transactionList = SLCBUtils.convertPaymentRequestDto(paymentRequestDTO);
                    File csvFile = CsvUtils.createCSVFile(transactionList, GSMATransaction.class);
                    String fileName = awsFileTransferService.uploadFile(csvFile);
                    logger.info("Uploaded CSV in S3 with name: " + fileName);
                });

    }
}
