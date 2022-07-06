package org.mifos.connector.slcb.zeebe;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.mifos.connector.slcb.dto.PaymentRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.Map;

import static org.mifos.connector.slcb.camel.config.CamelProperties.*;

@Component
public class ZeebeWorkers {

    public ZeebeWorkers(ObjectMapper objectMapper, ZeebeClient zeebeClient, ProducerTemplate producerTemplate, CamelContext camelContext) {
        this.objectMapper = objectMapper;
        this.zeebeClient = zeebeClient;
        this.producerTemplate = producerTemplate;
        this.camelContext = camelContext;
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ObjectMapper objectMapper;

    private final ZeebeClient zeebeClient;

    private final ProducerTemplate producerTemplate;

    private final CamelContext camelContext;

    @Value("${zeebe.client.evenly-allocated-max-jobs}")
    private int workerMaxJobs;

    @PostConstruct
    public void setupWorkers() {

        zeebeClient.newWorker()
                .jobType(Worker.SLCB_TRANSFER.toString())
                .handler((client, job) -> {
                    logger.info("Job '{}' started from process '{}' with key {}",
                            job.getType(), job.getBpmnProcessId(), job.getKey());
                    Map<String, Object> variables = job.getVariablesAsMap();

                    Exchange exchange = new DefaultExchange(camelContext);

                    String prop = (String) variables.get(SLCB_CHANNEL_REQUEST);
                    logger.info("SLCB_CHANNEL_REQUEST: " + prop);
                    PaymentRequestDTO SLCBChannelRequestBody = objectMapper.readValue(
                            prop, PaymentRequestDTO.class);

                    exchange.setProperty(SLCB_CHANNEL_REQUEST, SLCBChannelRequestBody);
                    exchange.setProperty(CORRELATION_ID, variables.get("transactionId"));
                    exchange.setProperty(ZEEBE_JOB_KEY, job.getKey());

                    producerTemplate.asyncSend("direct:transfer-route", exchange);

                })
                .name(Worker.SLCB_TRANSFER.toString())
                .maxJobsActive(workerMaxJobs)
                .open();

        zeebeClient.newWorker()
                .jobType(Worker.RECONCILIATION.toString())
                .handler((client, job) -> {
                    logger.info("Job '{}' started from process '{}' with key {}",
                            job.getType(), job.getBpmnProcessId(), job.getKey());

                    Map<String, Object> variables = job.getVariablesAsMap();

                    Exchange exchange = new DefaultExchange(camelContext);

                    String prop = (String) variables.get(SLCB_CHANNEL_REQUEST);
                    logger.info("SLCB_CHANNEL_REQUEST: " + prop);
                    PaymentRequestDTO SLCBChannelRequestBody = objectMapper.readValue(
                            prop, PaymentRequestDTO.class);

                    exchange.setProperty(SLCB_CHANNEL_REQUEST, SLCBChannelRequestBody);
                    exchange.setProperty(CORRELATION_ID, variables.get("transactionId"));
                    exchange.setProperty(ZEEBE_JOB_KEY, job.getKey());
                    exchange.setProperty(SOURCE_ACCOUNT, variables.get(ZeebeVariables.SOURCE_ACCOUNT));
                    exchange.setProperty(TOTAL_AMOUNT, variables.get(ZeebeVariables.TOTAL_AMOUNT));
                    exchange.setProperty(BATCH_ID, variables.get(ZeebeVariables.BATCH_ID));

                    producerTemplate.asyncSend("direct:reconciliation-route", exchange);

                })
                .name(Worker.RECONCILIATION.toString())
                .maxJobsActive(workerMaxJobs)
                .open();

    }

    protected enum Worker {
        SLCB_TRANSFER("initiateTransfer"),
        RECONCILIATION("reconciliation");

        private final String text;

        Worker(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
