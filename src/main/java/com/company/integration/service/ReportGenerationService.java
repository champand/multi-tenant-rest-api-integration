package com.company.integration.service;

import com.company.integration.mapper.ClientMapper;
import com.company.integration.model.dto.AuditReportDTO;
import com.company.integration.model.entity.ClientEmailRecipient;
import com.company.integration.util.CsvGenerator;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating and emailing daily compliance reports.
 * Triggered by external Windows Task Scheduler at 9:00 AM IST.
 */
@Service
public class ReportGenerationService {

    private static final Logger logger = LogManager.getLogger(ReportGenerationService.class);

    private final AuditService auditService;
    private final ClientMapper clientMapper;
    private final CsvGenerator csvGenerator;
    private final JavaMailSender mailSender;

    @Value("${email.from.address}")
    private String fromAddress;

    @Value("${email.from.name:Integration Service}")
    private String fromName;

    @Value("${report.date.format:yyyyMMdd}")
    private String dateFormat;

    public ReportGenerationService(AuditService auditService,
                                   ClientMapper clientMapper,
                                   CsvGenerator csvGenerator,
                                   JavaMailSender mailSender) {
        this.auditService = auditService;
        this.clientMapper = clientMapper;
        this.csvGenerator = csvGenerator;
        this.mailSender = mailSender;
    }

    /**
     * Generate and send daily reports for all clients.
     * This is the main entry point called by Windows Task Scheduler.
     *
     * @param reportDate the date for the report (typically yesterday)
     */
    public void generateAndSendDailyReports(LocalDate reportDate) {
        logger.info("Starting daily report generation for date: {}", reportDate);

        LocalDateTime startTime = reportDate.atStartOfDay();
        LocalDateTime endTime = reportDate.plusDays(1).atStartOfDay();

        // Find all clients with audit records
        List<String> clientsWithRecords = auditService.findClientsWithRecords(startTime, endTime);

        if (clientsWithRecords.isEmpty()) {
            logger.info("No audit records found for date: {}", reportDate);
            return;
        }

        logger.info("Found {} clients with audit records for {}", clientsWithRecords.size(), reportDate);

        // Generate and send report for each client
        for (String clientId : clientsWithRecords) {
            try {
                generateAndSendClientReport(clientId, reportDate, startTime, endTime);
            } catch (Exception e) {
                logger.error("Failed to generate/send report for client {}: {}", clientId, e.getMessage(), e);
            }
        }

        logger.info("Completed daily report generation for date: {}", reportDate);
    }

    /**
     * Generate and send report for a specific client.
     *
     * @param clientId   the client identifier
     * @param reportDate the report date
     * @param startTime  start of the time range
     * @param endTime    end of the time range
     */
    public void generateAndSendClientReport(String clientId, LocalDate reportDate,
                                            LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        logger.info("Generating report for client: {} for date: {}", clientId, reportDate);

        // Get client name
        String clientName = clientMapper.getClientName(clientId);
        if (clientName == null) {
            clientName = clientId;
        }

        // Get audit data
        List<AuditReportDTO> auditData = auditService.getReportData(clientId, startTime, endTime);

        if (auditData.isEmpty()) {
            logger.info("No audit data for client: {} on date: {}", clientId, reportDate);
            return;
        }

        // Set client name in each record
        for (AuditReportDTO dto : auditData) {
            dto.setClientName(clientName);
        }

        // Generate CSV file
        File csvFile = csvGenerator.generateAuditReport(clientId, clientName, auditData, reportDate);
        byte[] csvBytes = csvGenerator.generateCsvBytes(auditData);

        // Get audit statistics
        AuditService.AuditStats stats = auditService.getAuditStats(clientId, startTime, endTime);

        // Get email recipients
        List<ClientEmailRecipient> recipients = clientMapper.findEmailRecipients(clientId);

        if (recipients.isEmpty()) {
            logger.warn("No email recipients configured for client: {}", clientId);
            return;
        }

        // Send email
        sendReportEmail(clientId, clientName, reportDate, csvFile.getName(), csvBytes, recipients, stats);

        logger.info("Report generated and sent for client: {}", clientId);
    }

    /**
     * Send report email with CSV attachment.
     */
    @Async("emailExecutor")
    public void sendReportEmail(String clientId, String clientName, LocalDate reportDate,
                                String fileName, byte[] csvContent,
                                List<ClientEmailRecipient> recipients, AuditService.AuditStats stats) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set from
            helper.setFrom(fromAddress, fromName);

            // Set recipients
            List<String> toRecipients = recipients.stream()
                    .filter(r -> ClientEmailRecipient.TYPE_TO.equals(r.getRecipientType()))
                    .map(ClientEmailRecipient::getEmailAddress)
                    .collect(Collectors.toList());

            List<String> ccRecipients = recipients.stream()
                    .filter(r -> ClientEmailRecipient.TYPE_CC.equals(r.getRecipientType()))
                    .map(ClientEmailRecipient::getEmailAddress)
                    .collect(Collectors.toList());

            List<String> bccRecipients = recipients.stream()
                    .filter(r -> ClientEmailRecipient.TYPE_BCC.equals(r.getRecipientType()))
                    .map(ClientEmailRecipient::getEmailAddress)
                    .collect(Collectors.toList());

            if (toRecipients.isEmpty()) {
                logger.warn("No TO recipients for client: {}", clientId);
                return;
            }

            helper.setTo(toRecipients.toArray(new String[0]));

            if (!ccRecipients.isEmpty()) {
                helper.setCc(ccRecipients.toArray(new String[0]));
            }

            if (!bccRecipients.isEmpty()) {
                helper.setBcc(bccRecipients.toArray(new String[0]));
            }

            // Set subject
            String dateStr = reportDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            helper.setSubject(String.format("[Daily Audit Report] Client: %s - %s", clientName, dateStr));

            // Set body
            String body = buildEmailBody(clientName, reportDate, stats);
            helper.setText(body, true);

            // Attach CSV
            helper.addAttachment(fileName, new ByteArrayResource(csvContent), "text/csv");

            // Send
            mailSender.send(message);

            logger.info("Sent report email to {} recipients for client: {}", toRecipients.size(), clientId);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            logger.error("Failed to send report email for client {}: {}", clientId, e.getMessage(), e);
        }
    }

    /**
     * Build HTML email body with summary statistics.
     */
    private String buildEmailBody(String clientName, LocalDate reportDate, AuditService.AuditStats stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h2>Daily Audit Report</h2>");
        sb.append("<p><strong>Client:</strong> ").append(clientName).append("</p>");
        sb.append("<p><strong>Report Date:</strong> ")
                .append(reportDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))).append("</p>");

        sb.append("<h3>Summary</h3>");
        sb.append("<table border='1' cellpadding='5' cellspacing='0'>");
        sb.append("<tr><td><strong>Total API Calls</strong></td><td>").append(stats.getTotalCalls()).append("</td></tr>");
        sb.append("<tr><td><strong>Successful Calls</strong></td><td>").append(stats.getSuccessfulCalls()).append("</td></tr>");
        sb.append("<tr><td><strong>Failed Calls</strong></td><td>").append(stats.getFailedCalls()).append("</td></tr>");
        sb.append("<tr><td><strong>Success Rate</strong></td><td>")
                .append(String.format("%.2f%%", stats.getSuccessRate())).append("</td></tr>");
        sb.append("<tr><td><strong>Average Execution Time</strong></td><td>")
                .append(stats.getAverageExecutionTimeMs()).append(" ms</td></tr>");
        sb.append("</table>");

        sb.append("<p>Please find the detailed audit report attached.</p>");
        sb.append("<p><em>This is an automated message. Please do not reply.</em></p>");
        sb.append("</body></html>");

        return sb.toString();
    }

    /**
     * Generate report for yesterday (default behavior).
     */
    public void generateYesterdayReports() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        generateAndSendDailyReports(yesterday);
    }

    /**
     * Generate report for a specific date range.
     *
     * @param clientId  the client identifier
     * @param startDate start date
     * @param endDate   end date
     * @return File containing the report
     */
    public File generateCustomReport(String clientId, LocalDate startDate, LocalDate endDate) throws IOException {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();

        String clientName = clientMapper.getClientName(clientId);
        if (clientName == null) {
            clientName = clientId;
        }

        List<AuditReportDTO> auditData = auditService.getReportData(clientId, startTime, endTime);

        for (AuditReportDTO dto : auditData) {
            dto.setClientName(clientName);
        }

        return csvGenerator.generateAuditReport(clientId, clientName, auditData, endDate);
    }
}
