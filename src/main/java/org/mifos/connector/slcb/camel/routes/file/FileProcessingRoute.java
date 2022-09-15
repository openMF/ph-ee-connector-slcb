package org.mifos.connector.slcb.camel.routes.file;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.mifos.connector.slcb.camel.routes.transfer.BaseSLCBRouteBuilder;
import org.mifos.connector.slcb.dto.Transaction;
import org.mifos.connector.slcb.dto.TransactionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import static org.mifos.connector.slcb.camel.config.CamelProperties.*;

@Component
public class FileProcessingRoute extends BaseSLCBRouteBuilder {

    @Autowired
    private CsvMapper csvMapper;

    @Override
    public void configure() {

        /**
         * Parse the [Transaction] array from the csv file
         * exchangeInput: [LOCAL_FILE_PATH] the absolute path to the csv file
         * exchangeOutput: [TRANSACTION_LIST] containing the list of [Transaction]
         */
        from("direct:get-transaction-array")
                .id("direct:get-transaction-array")
                .log("Starting route direct:get-transaction-array")
                .process(exchange -> {
                    String filename = exchange.getProperty(LOCAL_FILE_PATH, String.class);
                    CsvSchema schema = CsvSchema.emptySchema().withHeader();
                    FileReader reader = new FileReader(filename);
                    MappingIterator<Transaction> readValues = csvMapper.readerWithSchemaFor(Transaction.class).with(schema).readValues(reader);
                    List<Transaction> transactionList = new ArrayList<>();
                    while (readValues.hasNext()) {
                        Transaction current = readValues.next();
                        transactionList.add(current);
                    }
                    reader.close();
                    exchange.setProperty(TRANSACTION_LIST, transactionList);
                });

        /**
         * updates the data in local file
         * exchangeInput:
         *      [LOCAL_FILE_PATH] the absolute path to the csv file
         *      [RESULT_TRANSACTION_LIST] containing the list of [Transaction]
         *      [OVERRIDE_HEADER] if set to true will override the header or else use the existing once in csv file
         */
        from("direct:update-result-file")
                .id("direct:update-result-file")
                .log("Starting route direct:update-result-file")
                .process(exchange -> {
                    String filepath = exchange.getProperty(LOCAL_FILE_PATH, String.class);
                    List<TransactionResult> transactionList = exchange.getProperty(RESULT_TRANSACTION_LIST, List.class);

                    // getting header
                    Boolean overrideHeader = exchange.getProperty(OVERRIDE_HEADER, Boolean.class);

                    csvWriter(transactionList, TransactionResult.class, csvMapper, overrideHeader, filepath);
                })
                .log("Update complete");

        /**
         * updates the data in local file
         * exchangeInput:
         *      [LOCAL_FILE_PATH] the absolute path to the csv file
         *      [TRANSACTION_LIST] containing the list of [Transaction]
         *      [OVERRIDE_HEADER] if set to true will override the header or else use the existing once in csv file
         */
        from("direct:update-file")
                .id("direct:update-file")
                .log("Starting route direct:update-file")
                .to("direct:update-file-v2")
                .log("Update complete");


        /**
         * this is backward compatible version of update-file route for new CSV schema
         * exchangeInput:
         *      [LOCAL_FILE_PATH] the absolute path to the csv file
         *      [TRANSACTION_LIST] containing the list of [Transaction]
         *      [OVERRIDE_HEADER] if set to true will override the header or else use the existing once in csv file
         */
        from("direct:update-file-v2")
                .id("direct:update-file-v2")
                .log("Starting route direct:update-file-v2")
                .process(exchange -> {
                    String filepath = exchange.getProperty(LOCAL_FILE_PATH, String.class);
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);

                    // getting header
                    Boolean overrideHeader = exchange.getProperty(OVERRIDE_HEADER, Boolean.class);

                    csvWriter(transactionList, Transaction.class, csvMapper, overrideHeader, filepath);
                });
    }

    private <T> void csvWriter(List<T> data, Class<T> tClass, CsvMapper csvMapper,
                               boolean overrideHeader, String filepath) throws IOException {
        CsvSchema csvSchema = csvMapper.schemaFor(tClass);
        if (overrideHeader) {
            csvSchema = csvSchema.withHeader();
        } else {
            csvSchema = csvSchema.withoutHeader();
        }

        File file = new File(filepath);
        SequenceWriter writer = csvMapper.writerWithSchemaFor(tClass).with(csvSchema).writeValues(file);
        for (T object: data) {
            writer.write(object);
        }
    }
}
