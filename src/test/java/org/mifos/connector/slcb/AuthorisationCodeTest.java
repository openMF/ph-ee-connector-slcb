package org.mifos.connector.slcb;

import com.google.common.collect.Range;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mifos.connector.slcb.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthorisationCodeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl = "http://localhost:";

    private String decryptionUrl = baseUrl + port + "/decrypt";

    private final String encryptionKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtPuP1m/jpt5O71c+5oNj" +
            "faZCCFuiz63936hoL2SfGvJtbRZjKZf5XHF8terfBzXb0W8HQ6o/204HOg/mm/fV" +
            "GjdM29kiIB26u12hWNlJybqNvcUWDww0i54OkXPtvVoDG2DyTz6sCy5Y3sTxyVB5" +
            "+hWs+lLeSyJ8cHzBH67HLfgsuZ+e3QNJkKtps/3Hkn7GE6gG4VApepdLNALvJY8v" +
            "2l70Xkc4R2RRPhfBl5pWF+pFAUcXHRarybr+irjFLpM86ph/K1g/EuZ/LPiYGciz" +
            "SR/U5jZhYDABzacPwEfqKILiLuzjCip3DpahfN0vZ/5Nel8+3y5zRVDDlamTqjjb" +
            "iwIDAQAB";

    @BeforeAll
    public void setDecryptionUrl() {
        this.decryptionUrl = baseUrl + port + "/decrypt";
    }

    @Test
    public void decryptionApiTest() {
        String validEncryptedData = "DW9kHyaF1K1TLnMY/3xV4zEuGvC3J0mAN4oRwrt/Yzh21ORsrw9QoGhFh" +
                "M4xoBDina1fXvRKqw50nFzhV1Fwz3nGEVTPfj48PsPJf/pxK+4OHa5wq40/xf91Q+DJ+7v2OUANqZe" +
                "EhfDhuJMTXo0r281S0a/rBmguqfzrZC986SV961UUZpehgAifAWKJShWu0t6Ur9Lui6u0YRhZn+/ho" +
                "YO4zHyfpa7Z5yMlhUZl/kLo0zDOc2jydH6kGUPqXOSnzvDRr8jXgLxsxex7vBrEVAGdrYKgMvY6Ecc" +
                "xvjekgAVrjbvBq48OaLouUVDUWZa1M6s1Q0i9KT0tO9ushkooEw==";

        ResponseEntity<String> response = restTemplate.postForEntity(decryptionUrl, validEncryptedData, String.class);

        assertThat(response.getStatusCodeValue()).isIn(Range.closed(200, 299));
        System.out.println("Decryption endpoint >> " +decryptionUrl + ", returned response code: " +
                response.getStatusCodeValue());
    }

    @Test
    public void authorisationCodeEncryptionAndDecryptionTest() {
        String data = String.valueOf(UUID.randomUUID());
        String authorisationCode;
        try {
            authorisationCode = SecurityUtils.encrypt(data, encryptionKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException | InvalidKeySpecException e) {
            assertThat(false).isTrue();
            e.printStackTrace();
            return;
        }
        String decryptedData = restTemplate.postForObject(decryptionUrl, authorisationCode, String.class);
        assertThat(decryptedData).isEqualTo(data);
        System.out.println("Initial data is same as decrypted data");
    }

}
