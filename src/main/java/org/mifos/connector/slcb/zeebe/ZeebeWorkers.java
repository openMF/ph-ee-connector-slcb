package org.mifos.connector.slcb.zeebe;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.Map;
import static org.mifos.connector.slcb.camel.config.CamelProperties.*;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.*;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.TRANSFER_FAILED;

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
                    logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
                    Map<String, Object> variables = job.getVariablesAsMap();

                    Exchange exchange = new DefaultExchange(camelContext);
                    exchange.setProperty(CORRELATION_ID, variables.get("transactionId"));
                    exchange.setProperty(SERVER_FILE_NAME, variables.get(FILE_NAME));
                    exchange.setProperty(BATCH_ID, variables.get(SUB_BATCH_ID));
                    exchange.setProperty(REQUEST_ID, variables.get(REQUEST_ID));
                    exchange.setProperty(PURPOSE, variables.get(PURPOSE));
                    exchange.setProperty(TOTAL_AMOUNT, variables.get(TOTAL_AMOUNT));
                    exchange.setProperty(ONGOING_AMOUNT, variables.get(ONGOING_AMOUNT));
                    exchange.setProperty(FAILED_AMOUNT, variables.get(FAILED_AMOUNT));
                    exchange.setProperty(COMPLETED_AMOUNT, variables.get(COMPLETED_AMOUNT));
                    exchange.setProperty(RESULT_FILE, variables.get(RESULT_FILE));

                    producerTemplate.send("direct:slcb-base", exchange);

                    Boolean transferFailed = exchange.getProperty(TRANSFER_FAILED, Boolean.class);

                    if (transferFailed == null || transferFailed) {
                        transferFailed = true;
                        variables.put(ERROR_CODE, exchange.getProperty(ERROR_CODE));
                        variables.put(ERROR_DESCRIPTION, exchange.getProperty(ERROR_DESCRIPTION));
                    } else {
                        variables.put(STATUS_CODE, exchange.getProperty(STATUS_CODE));
                        variables.put(STATUS_DESCRIPTION, exchange.getProperty(STATUS_DESCRIPTION));
                    }

                    variables.put(RECONCILIATION_ENABLED, exchange.getProperty(RECONCILIATION_ENABLED));
                    variables.put(TRANSFER_FAILED, transferFailed);
                    variables.put(TOTAL_TRANSACTION, exchange.getProperty(TOTAL_TRANSACTION));
                    variables.put(ONGOING_TRANSACTION, exchange.getProperty(ONGOING_TRANSACTION));
                    variables.put(FAILED_TRANSACTION, exchange.getProperty(FAILED_TRANSACTION));
                    variables.put(COMPLETED_TRANSACTION, exchange.getProperty(COMPLETED_TRANSACTION));
                    variables.put(ONGOING_AMOUNT, exchange.getProperty(ONGOING_AMOUNT));
                    variables.put(FAILED_AMOUNT, exchange.getProperty(FAILED_AMOUNT));
                    variables.put(COMPLETED_AMOUNT, exchange.getProperty(COMPLETED_AMOUNT));

                    zeebeClient.newCompleteCommand(job.getKey())
                            .variables(variables).send();
                })
                .name(Worker.SLCB_TRANSFER.toString())
                .maxJobsActive(workerMaxJobs)
                .open();

        zeebeClient.newWorker()
                .jobType(Worker.RECONCILIATION.toString())
                .handler((client, job) -> {
                    logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
                    Map<String, Object> variables = job.getVariablesAsMap();

                    variables.put(RECONCILIATION_RETRY, variables.getOrDefault(RECONCILIATION_RETRY, 1));

                    Exchange exchange = new DefaultExchange(camelContext);
                    exchange.setProperty(CORRELATION_ID, variables.get("transactionId"));
                    exchange.setProperty(SERVER_FILE_NAME, variables.get(FILE_NAME));
                    exchange.setProperty(BATCH_ID, variables.get(SUB_BATCH_ID));
                    exchange.setProperty(REQUEST_ID, variables.get(REQUEST_ID));
                    exchange.setProperty(PURPOSE, variables.get(PURPOSE));
                    exchange.setProperty(TOTAL_TRANSACTION, variables.get(TOTAL_TRANSACTION));
                    exchange.setProperty(ONGOING_TRANSACTION, variables.get(ONGOING_TRANSACTION));
                    exchange.setProperty(FAILED_TRANSACTION, variables.get(FAILED_TRANSACTION));
                    exchange.setProperty(COMPLETED_TRANSACTION, variables.get(COMPLETED_TRANSACTION));
                    exchange.setProperty(TOTAL_AMOUNT, variables.get(TOTAL_AMOUNT));
                    exchange.setProperty(ONGOING_AMOUNT, variables.get(ONGOING_AMOUNT));
                    exchange.setProperty(FAILED_AMOUNT, variables.get(FAILED_AMOUNT));
                    exchange.setProperty(COMPLETED_AMOUNT, variables.get(COMPLETED_AMOUNT));

                    producerTemplate.send("direct:reconciliation-route", exchange);

                    boolean isReconciliationSuccess = exchange.getProperty(RECONCILIATION_SUCCESS, Boolean.class);
                    variables.put(RECONCILIATION_SUCCESS, isReconciliationSuccess);

                    if (!isReconciliationSuccess) {
                        variables.put(ERROR_CODE, exchange.getProperty(ERROR_CODE));
                        variables.put(ERROR_DESCRIPTION, exchange.getProperty(ERROR_DESCRIPTION));
                    }

                    variables.put(TOTAL_TRANSACTION, exchange.getProperty(TOTAL_TRANSACTION));
                    variables.put(ONGOING_TRANSACTION, exchange.getProperty(TOTAL_TRANSACTION));
                    variables.put(FAILED_TRANSACTION, exchange.getProperty(TOTAL_TRANSACTION));
                    variables.put(COMPLETED_TRANSACTION, exchange.getProperty(TOTAL_TRANSACTION));
                    variables.put(ONGOING_AMOUNT, exchange.getProperty(ONGOING_AMOUNT));
                    variables.put(FAILED_AMOUNT, exchange.getProperty(FAILED_AMOUNT));
                    variables.put(COMPLETED_AMOUNT, exchange.getProperty(COMPLETED_AMOUNT));

                    logger.info("Setting zeebe variable: {}", variables);

                    zeebeClient.newCompleteCommand(job.getKey())
                            .variables(variables).send();
                })
                .name(Worker.SLCB_TRANSFER.toString())
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
