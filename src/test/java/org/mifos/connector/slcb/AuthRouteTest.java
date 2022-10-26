package org.mifos.connector.slcb;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.mifos.connector.slcb.camel.routes.auth.AuthRoutes;

import static org.apache.camel.reifier.RouteReifier.adviceWith;

@Ignore
public class AuthRouteTest extends CamelTestSupport {

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new AuthRoutes();
    }

    @Test
    public void mainTest() throws Exception {
        RouteDefinition routeDefinition = context.getRouteDefinition("get-access-token");

        adviceWith(routeDefinition, context,
                new AdviceWithRouteBuilder() {

                    @Override
                    public void configure() throws Exception {
                        weaveAddLast().to("mock:finish");
                    }
                });

        MockEndpoint mockEndpoint = getMockEndpoint("mock:finish");
        mockEndpoint.expectedMessageCount(1);

        //template.send("direct:get-access-token");
    }

}
