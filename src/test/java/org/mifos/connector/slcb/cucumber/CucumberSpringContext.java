package org.mifos.connector.slcb.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.Test;
import org.mifos.connector.slcb.SlcbConnectorApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static com.google.common.truth.Truth.assertThat;

@SpringBootTest
@CucumberContextConfiguration
@ActiveProfiles("test")
@ContextConfiguration(classes = SlcbConnectorApplication.class, loader = SpringBootContextLoader.class)
public class CucumberSpringContext {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Test
    void contextLoads() {
        assertThat(producerTemplate).isNotNull();
    }

}
