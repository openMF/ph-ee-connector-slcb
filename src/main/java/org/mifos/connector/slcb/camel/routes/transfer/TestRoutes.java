package org.mifos.connector.slcb.camel.routes.transfer;

import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.slcb.dto.PaymentRequestDTO;
import org.mifos.connector.slcb.file.FileTransferService;
import org.mifos.connector.slcb.utils.CsvUtils;
import org.mifos.connector.slcb.utils.SecurityUtils;
import org.mifos.connector.slcb.zeebe.ZeebeProcessStarter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import static org.mifos.connector.slcb.camel.config.CamelProperties.SLCB_CHANNEL_REQUEST;

@Component
public class TestRoutes extends BaseSLCBRouteBuilder {

    private final ZeebeProcessStarter zeebeProcessStarter;

    private final FileTransferService fileTransferService;

    public TestRoutes(ZeebeProcessStarter zeebeProcessStarter, @Qualifier("awsStorage") FileTransferService fileTransferService) {
        this.zeebeProcessStarter = zeebeProcessStarter;
        this.fileTransferService = fileTransferService;
    }

    @Override
    public void configure() throws Exception {
        from("rest:get:/start")
                .id("slcb-flow-start")
                .process(exchange -> {
                    try {
                        Map<String, Object> variables = new HashMap<>();
                        variables.put("transactionId", UUID.randomUUID());
                        zeebeProcessStarter.startZeebeWorkflow("SLCB", variables);
                    } catch (Exception e) {

                    }

                })
                .setBody(constant("Started"));

        from("rest:get:/test/file")
                .id("file-upload-service")
                .process(exchange -> {
                    List<PaymentRequestDTO> paymentRequestDTOList = new ArrayList<>();

                    PaymentRequestDTO paymentRequestDTO1 = new PaymentRequestDTO();
                    paymentRequestDTO1.setId("0");
                    paymentRequestDTO1.setAccountType(0);
                    paymentRequestDTO1.setAuthorizationCode("asd");

                    PaymentRequestDTO paymentRequestDTO2 = new PaymentRequestDTO();
                    paymentRequestDTO2.setId("1");
                    paymentRequestDTO2.setAccountType(1);
                    paymentRequestDTO2.setAuthorizationCode("demo auth code");

                    paymentRequestDTOList.add(paymentRequestDTO1);
                    paymentRequestDTOList.add(paymentRequestDTO2);


                    File file = CsvUtils.createCSVFile(paymentRequestDTOList, PaymentRequestDTO.class);
                    System.out.println(file.getAbsolutePath());
                    //String result = fileTransferService.uploadFile(file);
                    //exchange.getIn().setBody(result);
                });

        from("rest:post:test/transferRequest")
                .unmarshal().json(JsonLibrary.Jackson, PaymentRequestDTO.class)
                .process(exchange -> {
                    PaymentRequestDTO paymentRequestDTO = exchange.getIn().getBody(PaymentRequestDTO.class);
                    String authCode = SecurityUtils.signContent(UUID.randomUUID().toString(), slcbConfig.signatureKey);
                    authCode = Base64.getEncoder().encodeToString(authCode.getBytes());
                    logger.info("Signature: {}", slcbConfig.signatureKey);
                    logger.info("Auth Code: {}", authCode);
                    paymentRequestDTO.setAuthorizationCode(authCode);
                    exchange.setProperty(SLCB_CHANNEL_REQUEST, paymentRequestDTO);
                })
                .to("direct:get-access-token")
                .to("direct:commit-transaction")
                .setBody(exchange -> exchange.getIn().getBody(String.class));
    }
}
