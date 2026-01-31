package com.company.integration.util;

import com.company.integration.model.dto.AuditReportDTO;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility class for generating CSV reports from audit data.
 */
@Component
public class CsvGenerator {

    private static final Logger logger = LogManager.getLogger(CsvGenerator.class);

    @Value("${report.output.directory:./reports}")
    private String outputDirectory;

    @Value("${report.csv.delimiter:,}")
    private char delimiter;

    @Value("${report.date.format:yyyyMMdd}")
    private String dateFormat;

    /**
     * Generate a CSV report file from audit data.
     *
     * @param clientId   the client identifier
     * @param clientName the client name
     * @param auditData  list of audit report DTOs
     * @param reportDate the date for the report
     * @return File object pointing to the generated CSV
     * @throws IOException if file generation fails
     */
    public File generateAuditReport(String clientId, String clientName, List<AuditReportDTO> auditData,
                                    LocalDate reportDate) throws IOException {
        // Ensure output directory exists
        Path outputPath = Paths.get(outputDirectory);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
            logger.info("Created report output directory: {}", outputDirectory);
        }

        // Generate file name
        String dateStr = reportDate.format(DateTimeFormatter.ofPattern(dateFormat));
        String fileName = String.format("Client_Audit_Report_%s_%s.csv", clientId, dateStr);
        File outputFile = outputPath.resolve(fileName).toFile();

        // Generate CSV
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .builder()
                     .setDelimiter(delimiter)
                     .setHeader(AuditReportDTO.getCsvHeaders())
                     .build())) {

            // Write data rows
            for (AuditReportDTO record : auditData) {
                csvPrinter.printRecord((Object[]) record.toCsvRow());
            }

            csvPrinter.flush();
            logger.info("Generated audit report: {} with {} records", fileName, auditData.size());
        }

        return outputFile;
    }

    /**
     * Generate CSV content as a string (for email attachment).
     *
     * @param auditData list of audit report DTOs
     * @return CSV content as string
     * @throws IOException if generation fails
     */
    public String generateCsvContent(List<AuditReportDTO> auditData) throws IOException {
        StringWriter stringWriter = new StringWriter();

        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.DEFAULT
                .builder()
                .setDelimiter(delimiter)
                .setHeader(AuditReportDTO.getCsvHeaders())
                .build())) {

            for (AuditReportDTO record : auditData) {
                csvPrinter.printRecord((Object[]) record.toCsvRow());
            }

            csvPrinter.flush();
        }

        return stringWriter.toString();
    }

    /**
     * Generate CSV as byte array (for email attachment).
     *
     * @param auditData list of audit report DTOs
     * @return CSV content as byte array
     * @throws IOException if generation fails
     */
    public byte[] generateCsvBytes(List<AuditReportDTO> auditData) throws IOException {
        return generateCsvContent(auditData).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generate a summary report file.
     *
     * @param clientId   the client identifier
     * @param reportDate the date for the report
     * @param totalCalls total number of API calls
     * @param successfulCalls number of successful calls
     * @param failedCalls number of failed calls
     * @param avgExecutionTime average execution time in ms
     * @return File object pointing to the generated summary CSV
     * @throws IOException if file generation fails
     */
    public File generateSummaryReport(String clientId, LocalDate reportDate, int totalCalls,
                                      int successfulCalls, int failedCalls, long avgExecutionTime) throws IOException {
        Path outputPath = Paths.get(outputDirectory);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }

        String dateStr = reportDate.format(DateTimeFormatter.ofPattern(dateFormat));
        String fileName = String.format("Client_Summary_Report_%s_%s.csv", clientId, dateStr);
        File outputFile = outputPath.resolve(fileName).toFile();

        String[] headers = {"Client ID", "Report Date", "Total Calls", "Successful", "Failed",
                "Success Rate (%)", "Avg Execution Time (ms)"};

        double successRate = totalCalls > 0 ? (successfulCalls * 100.0 / totalCalls) : 0;

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .builder()
                     .setDelimiter(delimiter)
                     .setHeader(headers)
                     .build())) {

            csvPrinter.printRecord(clientId, dateStr, totalCalls, successfulCalls, failedCalls,
                    String.format("%.2f", successRate), avgExecutionTime);

            csvPrinter.flush();
            logger.info("Generated summary report: {}", fileName);
        }

        return outputFile;
    }

    /**
     * Clean up old report files.
     *
     * @param daysToKeep number of days to keep reports
     * @return number of files deleted
     * @throws IOException if cleanup fails
     */
    public int cleanupOldReports(int daysToKeep) throws IOException {
        Path outputPath = Paths.get(outputDirectory);
        if (!Files.exists(outputPath)) {
            return 0;
        }

        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
        int deletedCount = 0;

        File[] files = outputPath.toFile().listFiles((dir, name) -> name.endsWith(".csv"));
        if (files != null) {
            for (File file : files) {
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++;
                        logger.debug("Deleted old report file: {}", file.getName());
                    }
                }
            }
        }

        if (deletedCount > 0) {
            logger.info("Cleaned up {} old report files", deletedCount);
        }

        return deletedCount;
    }

    /**
     * Get the full path for a report file.
     *
     * @param clientId   the client identifier
     * @param reportDate the date for the report
     * @return full path to the report file
     */
    public String getReportFilePath(String clientId, LocalDate reportDate) {
        String dateStr = reportDate.format(DateTimeFormatter.ofPattern(dateFormat));
        String fileName = String.format("Client_Audit_Report_%s_%s.csv", clientId, dateStr);
        return Paths.get(outputDirectory, fileName).toString();
    }
}
