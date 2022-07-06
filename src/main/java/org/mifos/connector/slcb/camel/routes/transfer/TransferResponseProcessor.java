package org.mifos.connector.slcb.camel.routes.transfer;

import io.camunda.zeebe.client.ZeebeClient;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.mifos.connector.slcb.dto.PaymentRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;
import static org.mifos.connector.slcb.camel.config.CamelProperties.*;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.*;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.SOURCE_ACCOUNT;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.TOTAL_AMOUNT;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.TRANSACTION_FAILED;

@Component
public class TransferResponseProcessor implements Processor {

    @Autowired
    private ZeebeClient zeebeClient;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${zeebe.client.ttl}")
    private int timeToLive;

    @Override
    public void process(Exchange exchange) {

        Map<String, Object> variables = (Map<String, Object>) exchange.getProperty(ZEEBE_VARIABLES);

        PaymentRequestDTO paymentRequestDTO = exchange.getProperty(SLCB_TRANSACTION_RESPONSE,
                PaymentRequestDTO.class);
        variables.put(SOURCE_ACCOUNT, paymentRequestDTO.getSourceAccount());
        variables.put(TOTAL_AMOUNT, paymentRequestDTO.getTotalAmountToBePaid());
        variables.put(BATCH_ID, paymentRequestDTO.getBatchID());

        Object hasTransferFailed = exchange.getProperty(TRANSACTION_FAILED);

        if (hasTransferFailed != null && (boolean)hasTransferFailed) {
            variables.put(TRANSACTION_FAILED, true);
            variables.put(ERROR_INFORMATION, exchange.getIn().getBody(String.class));
        } else {
            variables.put(TRANSFER_STATE, "COMMITTED");
            variables.put(TRANSACTION_FAILED, false);
        }
        exchange.setProperty(ZEEBE_VARIABLES, variables);
    }
}
