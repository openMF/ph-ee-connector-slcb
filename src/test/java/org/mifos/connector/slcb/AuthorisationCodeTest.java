package org.mifos.connector.slcb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mifos.connector.slcb.utils.SecurityUtils;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;
import static com.google.common.truth.Truth.assertThat;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthorisationCodeTest {

    private final String encryptionKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtPuP1m/jpt5O71c+5oNj" +
            "faZCCFuiz63936hoL2SfGvJtbRZjKZf5XHF8terfBzXb0W8HQ6o/204HOg/mm/fV" +
            "GjdM29kiIB26u12hWNlJybqNvcUWDww0i54OkXPtvVoDG2DyTz6sCy5Y3sTxyVB5" +
            "+hWs+lLeSyJ8cHzBH67HLfgsuZ+e3QNJkKtps/3Hkn7GE6gG4VApepdLNALvJY8v" +
            "2l70Xkc4R2RRPhfBl5pWF+pFAUcXHRarybr+irjFLpM86ph/K1g/EuZ/LPiYGciz" +
            "SR/U5jZhYDABzacPwEfqKILiLuzjCip3DpahfN0vZ/5Nel8+3y5zRVDDlamTqjjb" +
            "iwIDAQAB";

    private final String decryptionKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC0+4/Wb+Om3k7v" +
            "Vz7mg2N9pkIIW6LPrf3fqGgvZJ8a8m1tFmMpl/lccXy16t8HNdvRbwdDqj/bTgc6" +
            "D+ab99UaN0zb2SIgHbq7XaFY2UnJuo29xRYPDDSLng6Rc+29WgMbYPJPPqwLLlje" +
            "xPHJUHn6Faz6Ut5LInxwfMEfrsct+Cy5n57dA0mQq2mz/ceSfsYTqAbhUCl6l0s0" +
            "Au8ljy/aXvReRzhHZFE+F8GXmlYX6kUBRxcdFqvJuv6KuMUukzzqmH8rWD8S5n8s" +
            "+JgZyLNJH9TmNmFgMAHNpw/AR+ooguIu7OMKKncOlqF83S9n/k16Xz7fLnNFUMOV" +
            "qZOqONuLAgMBAAECggEANNEtmxEwSOSb+LFng/JYOLUqlDHaA+3tJzaIoTwmSsDx" +
            "OmLMMblOZrIgCR8wU3ReYHKclhy7Yg8VgNZfIKllIa992LM3iFPkyQV8LufK5vpw" +
            "ny9DTsTrGMvZyI0ilp4MRhM24/WQU/sEqI6lWXEJB/kHcE563UaFNnbSDaL+MeW7" +
            "FgGT8lu8/IvTdTYPF5fj6ifcEn0hEZ/aLEg0m9DRWwIdcdqsJRIc0ugSeYTedtrd" +
            "p6TBoPNgrIL2Jde+cJndWVFAbvpHza7bq6ej9wZbUZx8ICB+VhicVHAfBxvQL9Yr" +
            "5B/ud8cUmmAg1t5Oe5JG8UIUD8oS2jD9bwj+NJKKUQKBgQDwBO205S4PhDkL55nZ" +
            "zLAC0CmmWMJhyAUpzar0YBo28K+L9Z3q7IfMmkIgaCQG1o0XIZYWpS0p13F1/ldL" +
            "JTIfjZGRDSjran48ILu54P1EPaLRGymwdBqsvCJITSTxaurXW0ekRsXkcjGaxzqG" +
            "3niZunCvDODVQBME4rgiVvJFZQKBgQDBCF5dqw+skPJTgUD9dude7f5qCDP//APT" +
            "H3qUwExLEmHnMPOkXD//C3mu+jLuHHvuULwl19CkuNE4jJ+GgbuEYnrmgx/X5r7R" +
            "RORYY/hZVcAhXZsuRwjaEuC9m9efvE3d4E9cDaiQIOweChsND0EVi9L4oJAhuXwR" +
            "8+9B+arGLwKBgFE+2dfp2/WUpFrLQuDe0JWjMPYGBYZj1puX6s5d2YHPZxzRP2tO" +
            "NYmkjc26crd92LSDwfJYZzlKnDV8qr/dD2Ju4V9gPQGzQpfH3MPGzPRUiNCPiUUZ" +
            "iA4AgPpIYsD1mBjd5RpOep4hqXjjB4SvudMPsSUQDusgjU+SDxJQrCGhAoGBAKfz" +
            "3hdlxSeCnjWl2qQulrV0Ic6kAIqT/cfuNbvDbR5Mij6bywGQ+mWw2Fk0fKfMxM/g" +
            "EzRiCLmpzPCE+jAQJNXU0dZK9KPnstNmO7/ki6s+/wKI7YJgcAU+M6kGNaBYOO/6" +
            "QVJ419c/rfGdHVhJk3lpxVBqc73EI32DXwNqdfolAoGAaCG2oOBtJViAPmcU9ZqC" +
            "MWAU2A2XL3OpYqLiqXunqrGi19oo0Yq19KzeGWQ+IYgE+GCNvYaFE0t9spil9WCP" +
            "PWCcv7w53R8KM0f3v7eqRTWCNDSXI1pfPJXduonIvqybAiG8nF8yPe7iyGlixY/v" +
            "znfJqXGq8xCTgFs8ACOWKeE=";

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
        String decryptedData = null;
        try {
            decryptedData = SecurityUtils.decrypt(authorisationCode, decryptionKey);
            assertThat(decryptedData).isEqualTo(data);
            System.out.println("Initial data is same as decrypted data");
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException | InvalidKeySpecException e) {
            assertThat(false).isTrue();
            e.printStackTrace();
            return;
        }
    }

}
