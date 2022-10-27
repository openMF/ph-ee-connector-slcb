package org.mifos.connector.slcb.cucumber;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import org.mifos.connector.slcb.dto.AccessTokenDTO;
import static com.google.common.truth.Truth.assertThat;
import static org.apache.camel.builder.AdviceWithRouteBuilder.adviceWith;
import static org.apache.camel.language.constant.ConstantLanguage.constant;
import static org.mifos.connector.slcb.camel.config.CamelProperties.SLCB_ACCESS_TOKEN;


public class AuthRouteTest extends BaseStepDef {

    @EndpointInject("mock:auth-response")
    protected MockEndpoint mockAuthBean;

    private final AccessTokenDTO accessTokenDTO = new AccessTokenDTO();

    private Exchange exchange;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @And("I can mock external API response with token: {string} and expiry: {int}")
    public void testAuthEndpoint(String token, int expiry) throws Exception {
        accessTokenDTO.setToken(token);
        accessTokenDTO.setExpiresIn(expiry);
        adviceWith(context, "get-access-token", a -> {
            a.weaveByToUri("direct:access-token-fetch").replace()
                    .setBody(exchange -> {
                        try {
                            return objectMapper.writeValueAsString(accessTokenDTO);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                    .to(mockAuthBean);
        });
        context.start();

        mockAuthBean.assertIsSatisfied();
    }

    @When("I call get-token route")
    public void callGetAccessTokenRoute() throws Exception {
        exchange = template.send("direct:get-access-token", new DefaultExchange(context));
        assertThat(exchange).isNotNull();
    }

    @Then("the exchange should have a variable with token {string}")
    public void checkToken(String token) {
        assertThat(exchange.getProperty(SLCB_ACCESS_TOKEN)).isNotNull();
        assertThat(exchange.getProperty(SLCB_ACCESS_TOKEN, String.class)).isEqualTo(token);
        logger.info("Token: {}", exchange.getProperty(SLCB_ACCESS_TOKEN));
    }

}
