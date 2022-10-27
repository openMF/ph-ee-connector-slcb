package org.mifos.connector.slcb.cucumber;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.camel.Exchange;
import org.mifos.connector.slcb.dto.Status;

import static com.google.common.truth.Truth.assertThat;
import static org.apache.camel.builder.AdviceWithRouteBuilder.adviceWith;
import static org.apache.camel.language.constant.ConstantLanguage.constant;
import static org.mifos.connector.slcb.camel.config.CamelProperties.SLCB_ACCESS_TOKEN;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.TRANSFER_FAILED;

public class TransferRouteStepDef extends BaseStepDef {

    @Given("I can mock get-access-token and external api call with status code {int}")
    public void mockAccessTokenAndExternalApiRequest(int httpResponse) throws Exception {
        adviceWith(context, "transfer-route", a -> {
            a.weaveByToUri("direct:get-access-token").replace()
                    .setProperty(SLCB_ACCESS_TOKEN, constant("token"));

            a.weaveByToUri("direct:commit-transaction").replace()
                    .process(ex -> {
                        Status status = new Status();
                        status.setCode(0);
                        status.setDescription("Request success");
                       paymentRequestDTO.setStatus(status);
                       ex.getIn().setBody(objectMapper.writeValueAsString(paymentRequestDTO));
                       ex.setProperty(Exchange.HTTP_RESPONSE_CODE, httpResponse);
                    });
        });

        adviceWith(context, "transaction-response-handler", a -> {
            a.weaveByToUri("direct:update-result-file").remove();
            a.weaveByToUri("direct:upload-file").remove();
            a.weaveByToUri("direct:delete-local-file").remove();
        });

        context.start();
    }

    @When("I call the transfer route")
    public void callTransferRoute() throws Exception {
        exchange = template.send("direct:transfer-route", exchange);
        assertThat(exchange).isNotNull();
    }

    @Then("the transfer failed variable should be {string}")
    public void checkTransferFailed(String transferFailed) {
        assertThat(exchange.getProperty(TRANSFER_FAILED)).isNotNull();
        if (transferFailed.equals("true")) {
            assertThat(exchange.getProperty(TRANSFER_FAILED, Boolean.class)).isTrue();
        } else {
            assertThat(exchange.getProperty(TRANSFER_FAILED, Boolean.class)).isFalse();
        }
    }


}
