package org.mifos.connector.slcb.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

// this class is the base for all the cucumber step definitions
public class BaseStepDef {

    @Autowired
    ProducerTemplate template;


    @Autowired
    CamelContext context;

    @Autowired
    ObjectMapper objectMapper;

    Logger logger = LoggerFactory.getLogger(this.getClass());

}
