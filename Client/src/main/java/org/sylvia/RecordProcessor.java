package org.sylvia;

import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class RecordProcessor {
    private FileWriter fileWriter;
    private LinkedBlockingDeque<Record> records;

    public RecordProcessor(String filePath, LinkedBlockingDeque<Record> records) throws IOException {
        this.records = records;
        generateReport();
        writeToCSV(filePath);
    }

    // Calculate matrix
    private void generateReport() {
        List<Record> recorderList = new ArrayList<>();
        long totalDuration = 0;
        for(Record record : records) {
            recorderList.add(record);
            totalDuration += record.getLatency();
        }
        int index = (int) Math.ceil(99 / 100.0 * recorderList.size()) - 1;
        long p99Latency = recorderList.get(index).getLatency();

        Collections.sort(recorderList, (r1, r2) -> (int) (r1.getLatency() - r2.getLatency()));

        long minLatency = recorderList.get(0).getLatency();
        long maxLatency = recorderList.get(recorderList.size() - 1).getLatency();
        long medianLatency = recorderList.get(recorderList.size() / 2).getLatency();
        double meanLatency = (double) totalDuration / recorderList.size();

        System.out.println("---------------------- Report Client2 ----------------------");
        System.out.println("mean response time (milliseconds): " + meanLatency);
        System.out.println("median response time (milliseconds): " + medianLatency);
        System.out.println("p99 (99th percentile) response time: " + p99Latency);
        System.out.println("min response time (milliseconds): " + minLatency);
        System.out.println("max response time (milliseconds): " + maxLatency);
        System.out.println("---------------------- Report End ----------------------");
    }

    private void writeToCSV(String filePath) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(filePath));
        String[] header = {"Start Time", "Request Type", "Latency", "Response Code"};
        writer.writeNext(header);
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        for(Record record : records){
            String[] temp = new String[4];
            temp[0] = ft.format(new Date(record.getStartTime())) + "\t";
            temp[0] = record.getStartTime() + "\t";
            temp[1] = record.getRequestType();
            temp[2] = record.getLatency() + "\t";
            temp[3] = String.valueOf(record.getResponseCode());
            writer.writeNext(temp);
        }
        writer.close();
    }
}
