package org.mifos.connector.slcb.camel.routes.transfer;

import org.mifos.connector.slcb.dto.Payee;
import org.mifos.connector.slcb.dto.PaymentRequestDTO;
import org.mifos.connector.slcb.dto.Transaction;
import org.mifos.connector.slcb.utils.SLCBUtils;
import org.mifos.connector.slcb.utils.SecurityUtils;
import org.springframework.stereotype.Component;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.mifos.connector.slcb.camel.config.CamelProperties.SLCB_CHANNEL_REQUEST;
import static org.mifos.connector.slcb.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.*;

@Component
public class SLCBBaseRoutes extends BaseSLCBRouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:slcb-base")
                .id("direct:slcb-base")
                .log("Starting route direct:slcb-base")
                .to("direct:download-file")
                .to("direct:get-transaction-array")
                .to("direct:build-payload")
                .to("direct:transfer-route");

        from("direct:build-payload")
                .id("direct:build-payload")
                .log("Starting route direct:build-payload")
                .process(exchange -> {
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    String batchId = exchange.getProperty(BATCH_ID, String.class);
                    String requestId = exchange.getProperty(REQUEST_ID, String.class);
                    String purpose = exchange.getProperty(PURPOSE, String.class);
                    String authCode = SecurityUtils.signContent(UUID.randomUUID().toString(), slcbConfig.signatureKey);

                    SimpleDateFormat format = new SimpleDateFormat(slcbConfig.dateFormat);
                    String requestDate = format.format(new Date());

                    PaymentRequestDTO paymentRequestDTO = new PaymentRequestDTO();
                    paymentRequestDTO.setId(requestId);
                    paymentRequestDTO.setAccountType(slcbConfig.accountType);
                    paymentRequestDTO.setAuthorizationCode(authCode);
                    paymentRequestDTO.setBatchID(batchId);
                    paymentRequestDTO.setInstitutionCode(slcbConfig.institutionCode);
                    paymentRequestDTO.setPurpose(purpose);
                    paymentRequestDTO.setRequestDate(requestDate);
                    if (transactionList != null && !transactionList.isEmpty() &&
                            transactionList.get(0).getPayerIdentifier() != null &&
                            !transactionList.get(0).getPayerIdentifier().isEmpty()) {
                        paymentRequestDTO.setSourceAccount(transactionList.get(0).getPayerIdentifier());
                        logger.info("Great you are using the newer specs");
                    } else {
                        paymentRequestDTO.setSourceAccount(slcbConfig.sourceAccount);
                        logger.info("Older specs!!! No worry we are compatible");
                    }
                    paymentRequestDTO.setTotalAmountPaid(0.0);

                    double amountToBePaid = 0.0;
                    List<Payee> payees = new ArrayList<>();
                    for (Transaction transaction: transactionList) {
                        amountToBePaid += Double.parseDouble(transaction.getAmount());
                        Payee payee = SLCBUtils.convertTransactionToPayee(transaction);
                        payees.add(payee);
                    }

                    paymentRequestDTO.setTotalAmountToBePaid(amountToBePaid);
                    paymentRequestDTO.setPayees(payees);

                    logger.info("Payment Request DTO: {}", paymentRequestDTO.toString());
                    exchange.setProperty(SLCB_CHANNEL_REQUEST, paymentRequestDTO);
                    exchange.setProperty(RECONCILIATION_ENABLED, slcbConfig.isReconciliationEnabled);
                    exchange.setProperty(TOTAL_TRANSACTION, transactionList.size());
                    exchange.setProperty(ONGOING_TRANSACTION, transactionList.size());
                    exchange.setProperty(FAILED_TRANSACTION, 0);
                    exchange.setProperty(COMPLETED_TRANSACTION, 0);
                });
    }
}
