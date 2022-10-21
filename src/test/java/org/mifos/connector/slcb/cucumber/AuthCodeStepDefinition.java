package org.mifos.connector.slcb.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.mifos.connector.slcb.utils.SecurityUtils;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;
import static com.google.common.truth.Truth.assertThat;

@CucumberContextConfiguration
public class AuthCodeStepDefinition {

    private String randomData;
    private String encryptedData;
    private String decryptedData;

    @Given("generate random data")
    public void generateRandomData() {
        // Write code here that turns the phrase above into concrete actions
        randomData = UUID.randomUUID().toString();
        System.out.println("Random data: " + randomData);
    }

    @When("encrypt the data with the {string}")
    public void encryptTheDataWithThe(String encryptionKey) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeySpecException, InvalidKeyException {
        encryptedData = SecurityUtils.encrypt(randomData, encryptionKey);
        System.out.println("Encrypted data: " + encryptedData);
    }

    @Then("encrypted data is not null")
    public void encryptedDataIsNotNull() {
        assertThat(encryptedData).isNotNull();
    }

    @When("encrypted data is decrypted using the {string}")
    public void encryptedDataIsDecryptedUsingThe(String decryptionKey) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeySpecException, InvalidKeyException {
        decryptedData = SecurityUtils.decrypt(encryptedData, decryptionKey);
        System.out.println("Decrypted data: " + decryptedData);
    }

    @Then("compare the decrypted data with the original data")
    public void compareTheDecryptedDataWithTheOriginalData() {
        assertThat(decryptedData).isEqualTo(randomData);
    }

}
