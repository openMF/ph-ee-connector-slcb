package org.mifos.connector.slcb.camel.config;

public class CamelProperties {

    private CamelProperties() {}

    public static final String CORRELATION_ID = "correlationId";
    public static final String ERROR_INFORMATION = "errorInformation";

    public static final String SERVER_FILE_NAME = "serverFileName";

    public static final String LOCAL_FILE_PATH = "localFilePath";

    public static final String TRANSACTION_LIST = "transactionList";

    public static final String RESULT_TRANSACTION_LIST = "resultTransactionList";

    public static final String OVERRIDE_HEADER = "overrideHeader";

    public static final String TRANSACTION_ID = "transactionId";
    public static final String SLCB_ACCESS_TOKEN = "slcbAccessToken";
    public static final String SLCB_CHANNEL_REQUEST = "slcbChannelRequest";
    public static final String SLCB_BALANCE_REQUEST = "slcbBalanceRequest";
    public static final String SLCB_RECONCILIATION_REQUEST = "slcbReconciliationRequest";

    public static final String SLCB_TRANSACTION_RESPONSE = "slcbTransactionResponse";

    public static final String TEST = "test";
    public static final String ZEEBE_JOB_KEY = "jobKey";

    public static final String ZEEBE_VARIABLES = "zeebeVariables";
}
