package org.mifos.connector.slcb.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.mifos.connector.slcb.dto.AccessTokenDTO;
import org.mifos.connector.slcb.dto.PaymentRequestDTO;
import org.mifos.connector.slcb.dto.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

// this class is the base for all the cucumber step definitions
public class BaseStepDef {

    @Autowired
    ProducerTemplate template;


    @Autowired
    CamelContext context;

    @Autowired
    ObjectMapper objectMapper;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final AccessTokenDTO accessTokenDTO = new AccessTokenDTO();

    protected static String batchId;
    protected static String requestId;
    protected static String purpose;
    protected static List<Transaction> transactionList;

    protected static Exchange exchange;

    protected static PaymentRequestDTO paymentRequestDTO;

}
