package org.mifos.connector.slcb.utils;

import org.mifos.connector.slcb.dto.Transaction;
import org.mifos.connector.slcb.dto.TransactionResult;

public class TransactionUtils {

    public static TransactionResult mapToResultDTO(Transaction transaction) {
        TransactionResult transactionResult = new TransactionResult();
        transactionResult.setId(transaction.getId());
        transactionResult.setRequestId(transaction.getRequestId());
        transactionResult.setPaymentMode(transaction.getPaymentMode());
        transactionResult.setPayerIdentifierType("accountNumber");
        transactionResult.setPayerIdentifier(transaction.getPayerIdentifier());
        transactionResult.setAmount(transaction.getAmount());
        transactionResult.setCurrency(transactionResult.getCurrency());
        transactionResult.setNote(transactionResult.getNote());
        transactionResult.setPayeeIdentifierType("accountNumber");
        transactionResult.setPayeeIdentifier(transaction.getAccountNumber());
        transactionResult.setProgramShortCode(transaction.getProgramShortCode());
        transactionResult.setCycle(transactionResult.getCycle());
        return transactionResult;
    }
}
