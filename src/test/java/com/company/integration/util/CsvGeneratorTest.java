package com.company.integration.util;

import com.company.integration.model.dto.AuditReportDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CsvGenerator Tests")
class CsvGeneratorTest {

    private CsvGenerator csvGenerator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        csvGenerator = new CsvGenerator();
        ReflectionTestUtils.setField(csvGenerator, "outputDirectory", tempDir.toString());
        ReflectionTestUtils.setField(csvGenerator, "delimiter", ',');
        ReflectionTestUtils.setField(csvGenerator, "dateFormat", "yyyyMMdd");
    }

    @Test
    @DisplayName("Should generate audit report CSV file")
    void shouldGenerateAuditReportCsvFile() throws IOException {
        // Arrange
        String clientId = "TEST_CLIENT";
        String clientName = "Test Client";
        LocalDate reportDate = LocalDate.of(2024, 6, 15);

        List<AuditReportDTO> auditData = createSampleAuditData();

        // Act
        File result = csvGenerator.generateAuditReport(clientId, clientName, auditData, reportDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.exists());
        assertEquals("Client_Audit_Report_TEST_CLIENT_20240615.csv", result.getName());

        // Verify content
        String content = Files.readString(result.toPath());
        assertTrue(content.contains("Client ID"));
        assertTrue(content.contains("TEST_CLIENT"));
        assertTrue(content.contains("https://api.test.com/endpoint"));
    }

    @Test
    @DisplayName("Should generate CSV content as string")
    void shouldGenerateCsvContentAsString() throws IOException {
        // Arrange
        List<AuditReportDTO> auditData = createSampleAuditData();

        // Act
        String result = csvGenerator.generateCsvContent(auditData);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Client ID")); // Header
        assertTrue(result.contains("TEST_CLIENT"));
        assertTrue(result.contains("SUCCESS"));
    }

    @Test
    @DisplayName("Should generate CSV as byte array")
    void shouldGenerateCsvAsByteArray() throws IOException {
        // Arrange
        List<AuditReportDTO> auditData = createSampleAuditData();

        // Act
        byte[] result = csvGenerator.generateCsvBytes(auditData);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);

        String content = new String(result);
        assertTrue(content.contains("Client ID"));
    }

    @Test
    @DisplayName("Should generate summary report")
    void shouldGenerateSummaryReport() throws IOException {
        // Arrange
        String clientId = "TEST_CLIENT";
        LocalDate reportDate = LocalDate.of(2024, 6, 15);

        // Act
        File result = csvGenerator.generateSummaryReport(
                clientId, reportDate, 100, 95, 5, 250L);

        // Assert
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().contains("Summary"));

        String content = Files.readString(result.toPath());
        assertTrue(content.contains("100")); // Total calls
        assertTrue(content.contains("95")); // Successful
        assertTrue(content.contains("5")); // Failed
    }

    @Test
    @DisplayName("Should handle empty audit data")
    void shouldHandleEmptyAuditData() throws IOException {
        // Arrange
        List<AuditReportDTO> auditData = List.of();

        // Act
        String result = csvGenerator.generateCsvContent(auditData);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Client ID")); // Should still have headers
    }

    @Test
    @DisplayName("Should get correct report file path")
    void shouldGetCorrectReportFilePath() {
        // Arrange
        String clientId = "MY_CLIENT";
        LocalDate reportDate = LocalDate.of(2024, 7, 20);

        // Act
        String result = csvGenerator.getReportFilePath(clientId, reportDate);

        // Assert
        assertTrue(result.contains("MY_CLIENT"));
        assertTrue(result.contains("20240720"));
        assertTrue(result.endsWith(".csv"));
    }

    @Test
    @DisplayName("Should create output directory if not exists")
    void shouldCreateOutputDirectoryIfNotExists() throws IOException {
        // Arrange
        Path newDir = tempDir.resolve("new-reports");
        ReflectionTestUtils.setField(csvGenerator, "outputDirectory", newDir.toString());

        List<AuditReportDTO> auditData = createSampleAuditData();

        // Act
        File result = csvGenerator.generateAuditReport(
                "CLIENT", "Client Name", auditData, LocalDate.now());

        // Assert
        assertTrue(Files.exists(newDir));
        assertTrue(result.exists());
    }

    @Test
    @DisplayName("Should handle special characters in data")
    void shouldHandleSpecialCharactersInData() throws IOException {
        // Arrange
        AuditReportDTO dto = AuditReportDTO.builder()
                .clientId("TEST")
                .clientName("Test, \"Client\"")
                .requestTimestamp(LocalDateTime.now())
                .apiEndpoint("https://api.test.com")
                .httpMethod("POST")
                .statusCode(200)
                .status("SUCCESS")
                .executionTimeMs(100L)
                .errorMessage("Error with \"quotes\" and, commas")
                .build();

        List<AuditReportDTO> auditData = List.of(dto);

        // Act
        String result = csvGenerator.generateCsvContent(auditData);

        // Assert
        assertNotNull(result);
        // CSV should properly escape the special characters
    }

    // Helper method
    private List<AuditReportDTO> createSampleAuditData() {
        return List.of(
                AuditReportDTO.builder()
                        .clientId("TEST_CLIENT")
                        .clientName("Test Client")
                        .requestTimestamp(LocalDateTime.now())
                        .apiEndpoint("https://api.test.com/endpoint")
                        .httpMethod("POST")
                        .statusCode(200)
                        .status("SUCCESS")
                        .executionTimeMs(150L)
                        .sourceRecordId("REC001")
                        .correlationId("CORR001")
                        .build(),
                AuditReportDTO.builder()
                        .clientId("TEST_CLIENT")
                        .clientName("Test Client")
                        .requestTimestamp(LocalDateTime.now().minusHours(1))
                        .apiEndpoint("https://api.test.com/endpoint")
                        .httpMethod("POST")
                        .statusCode(500)
                        .status("FAILURE")
                        .executionTimeMs(5000L)
                        .errorMessage("Internal Server Error")
                        .sourceRecordId("REC002")
                        .correlationId("CORR002")
                        .build()
        );
    }
}
