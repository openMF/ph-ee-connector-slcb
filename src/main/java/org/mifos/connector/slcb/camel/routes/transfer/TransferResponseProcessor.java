package org.mifos.connector.slcb.camel.routes.transfer;

import io.camunda.zeebe.client.ZeebeClient;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;

import static org.mifos.connector.slcb.camel.config.CamelProperties.*;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.*;

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

        Object hasTransferFailed = exchange.getProperty(TRANSFER_FAILED);

        if (hasTransferFailed != null && (boolean)hasTransferFailed) {
            variables.put(TRANSFER_FAILED, true);
            variables.put(ERROR_INFORMATION, exchange.getIn().getBody(String.class));
        } else {
            variables.put(TRANSFER_STATE, "COMMITTED");
            variables.put(TRANSFER_FAILED, false);

            zeebeClient.newPublishMessageCommand()
                    .messageName(TRANSFER_MESSAGE)
                    .correlationKey(exchange.getProperty(TRANSACTION_ID, String.class))
                    .variables(variables)
                    .send()
                    .join();
        }
        exchange.setProperty(ZEEBE_VARIABLES, variables);
    }
}
