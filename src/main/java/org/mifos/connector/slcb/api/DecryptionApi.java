package org.mifos.connector.slcb.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface DecryptionApi {

    @PostMapping("/decrypt")
    String decrypt(@RequestBody String encryptedData);

}
