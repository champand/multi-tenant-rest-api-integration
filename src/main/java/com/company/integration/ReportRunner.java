package com.company.integration;

import com.company.integration.service.ReportGenerationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Standalone application for generating daily compliance reports.
 * Intended to be triggered by Windows Task Scheduler at 9:00 AM IST.
 *
 * Usage: java -jar integration-service.jar --spring.profiles.active=report [date]
 * Date format: yyyy-MM-dd (defaults to yesterday if not provided)
 */
@SpringBootApplication
@Profile("report")
public class ReportRunner implements CommandLineRunner {

    private static final Logger logger = LogManager.getLogger(ReportRunner.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportGenerationService reportGenerationService;

    public ReportRunner(ReportGenerationService reportGenerationService) {
        this.reportGenerationService = reportGenerationService;
    }

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "report");
        SpringApplication app = new SpringApplication(ReportRunner.class);
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting Daily Report Generation");

        LocalDate reportDate;

        if (args.length > 0) {
            try {
                reportDate = LocalDate.parse(args[0], DATE_FORMAT);
                logger.info("Using provided date: {}", reportDate);
            } catch (Exception e) {
                logger.warn("Invalid date format '{}', using yesterday", args[0]);
                reportDate = LocalDate.now().minusDays(1);
            }
        } else {
            reportDate = LocalDate.now().minusDays(1);
            logger.info("No date provided, using yesterday: {}", reportDate);
        }

        try {
            reportGenerationService.generateAndSendDailyReports(reportDate);
            logger.info("Report generation completed successfully");
        } catch (Exception e) {
            logger.error("Report generation failed: {}", e.getMessage(), e);
            System.exit(1);
        }

        logger.info("Report Runner completed");
    }
}
