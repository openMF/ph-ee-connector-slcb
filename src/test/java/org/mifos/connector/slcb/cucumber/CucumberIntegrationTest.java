package org.mifos.connector.slcb.cucumber;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/test/java/resources", glue = "org.mifos.connector.slcb.cucumber")
public class CucumberIntegrationTest {

}
