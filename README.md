# Multi-Tenant REST API Integration Service

A production-ready, multi-tenant REST API integration service built with Spring Boot 3.x and Java 21. This service dynamically invokes external client REST APIs using database-driven field mappings with full audit trail and compliance reporting.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Database Schema](#database-schema)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

## Overview

This service provides a client-agnostic integration platform that:
- Dynamically invokes external client REST APIs using database-driven field mappings
- Supports 100+ clients with 50+ daily API calls per client (~5000+ daily transactions)
- Provides full audit trail and compliance reporting
- Supports horizontal scaling with stateless design

## Features

- **Dynamic Field Mapping**: Database-driven configuration for mapping source fields to API payloads
- **Nested JSON Construction**: Support for up to 5 levels of nested JSON structures
- **Data Transformations**: Date formatting, string concatenation, type conversions, and more
- **Retry Mechanism**: Failed calls retry every 1 hour for up to 15 days (360 attempts)
- **Complete Audit Trail**: Synchronous audit logging for every API call
- **Daily Reports**: CSV reports emailed to configured recipients
- **AES-256 Encryption**: Secure storage for API keys and credentials
- **Health Monitoring**: Spring Boot Actuator endpoints for observability

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        REST Controller                          │
│                    IntegrationController                        │
└─────────────────────────┬───────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│                     Service Layer                               │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────┐ │
│  │ Integration │  │   Payload    │  │      RestApi           │ │
│  │   Service   │──│   Builder    │──│    Invocation          │ │
│  └─────────────┘  │   Service    │  │      Service           │ │
│         │         └──────────────┘  └────────────────────────┘ │
│         │                                      │               │
│  ┌──────▼──────┐  ┌──────────────┐  ┌─────────▼─────────────┐ │
│  │   Audit     │  │    Retry     │  │       Report          │ │
│  │   Service   │  │   Service    │  │    Generation         │ │
│  └─────────────┘  └──────────────┘  └───────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│                   MyBatis Mapper Layer                          │
│  ClientMapper │ FieldMappingMapper │ AuditMapper │ FailedCall  │
└─────────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│                     Oracle Database                             │
└─────────────────────────────────────────────────────────────────┘
```

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.x |
| ORM | MyBatis 3.x |
| Database | Oracle |
| HTTP Client | WebClient (Spring WebFlux) |
| Logging | Log4j2 |
| Encryption | Jasypt + AES-256 |
| Build Tool | Maven |
| Testing | JUnit 5 + Mockito |

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- Oracle Database
- SMTP server (for email reports)

### Build

```bash
# Clone the repository
git clone https://github.com/champand/multi-tenant-rest-api-integration.git
cd multi-tenant-rest-api-integration

# Build the project
mvn clean install

# Run tests
mvn test

# Build without tests
mvn clean package -DskipTests
```

### Database Setup

1. Create the database schema:
```bash
sqlplus user/password@database @scripts/01_create_tables.sql
```

2. Load sample data (optional):
```bash
sqlplus user/password@database @scripts/02_sample_data.sql
```

### Run the Application

```bash
# Set environment variables
export JASYPT_MASTER_PASSWORD=your-master-password
export AES_SECRET_KEY=your-32-char-aes-key-here-123

# Run the application
java -jar target/multi-tenant-integration-1.0.0-SNAPSHOT.jar
```

### Run Daily Report Generator

```bash
# Generate report for yesterday
java -jar target/multi-tenant-integration-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=report

# Generate report for specific date
java -jar target/multi-tenant-integration-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=report 2024-06-15
```

## Configuration

### Application Properties

Key configuration properties in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:oracle:thin:@//host:port/service
spring.datasource.username=ENC(encrypted_username)
spring.datasource.password=ENC(encrypted_password)

# REST Client
rest.client.timeout.seconds=300
rest.client.max.connections=100

# Retry Configuration
retry.max.attempts=360
retry.interval.hours=1

# Email
spring.mail.host=smtp.company.com
spring.mail.port=587

# Encryption
jasypt.encryptor.password=${JASYPT_MASTER_PASSWORD}
```

### Encrypting Credentials

Use Jasypt to encrypt sensitive values:

```bash
# Encrypt a password
java -cp jasypt-1.9.3.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI \
  input="your-password" password="master-password" algorithm=PBEWithMD5AndDES

# Use in properties file
spring.datasource.password=ENC(encrypted_value)
```

## API Reference

### Invoke API

```http
POST /api/v1/integration/invoke
Content-Type: application/json

{
  "clientId": "CRM_CLIENT_001",
  "sourceRecordId": "CUST001",
  "requestedBy": "SYSTEM"
}
```

**Response:**
```json
{
  "responseId": "audit-uuid",
  "correlationId": "correlation-uuid",
  "clientId": "CRM_CLIENT_001",
  "success": true,
  "statusCode": 200,
  "responseBody": { ... },
  "executionTimeMs": 150
}
```

### Batch Processing

```http
POST /api/v1/integration/batch/{clientId}
Content-Type: application/json

["RECORD_001", "RECORD_002", "RECORD_003"]
```

### Validate Request

```http
POST /api/v1/integration/validate
Content-Type: application/json

{
  "clientId": "CRM_CLIENT_001",
  "sourceRecordId": "CUST001"
}
```

### Get Audit Statistics

```http
GET /api/v1/integration/audit/{clientId}?date=2024-06-15
```

### Retry Management

```http
# Get pending retries
GET /api/v1/integration/retry/{clientId}

# Get retry queue stats
GET /api/v1/integration/retry/stats

# Trigger manual retry
POST /api/v1/integration/retry/{callId}/trigger

# Cancel pending retry
DELETE /api/v1/integration/retry/{callId}
```

### Health Check

```http
GET /api/v1/integration/health
```

## Database Schema

### Core Tables

| Table | Description |
|-------|-------------|
| `CLIENT_CONFIGURATION` | Client API endpoint configurations |
| `CLIENT_EMAIL_RECIPIENTS` | Email recipients for reports |
| `FIELD_MAPPING` | Source-to-target field mappings |
| `AUDIT_LOG` | Complete API call audit trail |
| `FAILED_API_CALLS` | Retry queue for failed calls |

### Field Mapping Configuration

Example mapping for nested JSON:

| SOURCE_TABLE | SOURCE_COLUMN | TARGET_FIELD_PATH | TRANSFORMATION_RULE |
|--------------|---------------|-------------------|---------------------|
| CUSTOMER | FIRST_NAME | customer.name.firstName | TRIM\|\|UPPERCASE |
| CUSTOMER | LAST_NAME | customer.name.lastName | TRIM\|\|UPPERCASE |
| CUSTOMER | DATE_OF_BIRTH | customer.dateOfBirth | DATE:yyyy-MM-dd |
| CUSTOMER | PHONE | customer.contact.phone | REPLACE:-> |

### Transformation Rules

| Rule | Example | Description |
|------|---------|-------------|
| `DATE:format` | `DATE:yyyy-MM-dd` | Format date |
| `CONCAT:fields` | `CONCAT:FIRST\|LAST\| ` | Concatenate fields |
| `TRIM` | `TRIM` | Remove whitespace |
| `UPPERCASE` | `UPPERCASE` | Convert to uppercase |
| `LOWERCASE` | `LOWERCASE` | Convert to lowercase |
| `REPLACE:old>new` | `REPLACE:->` | Replace characters |
| `SUBSTRING:start,end` | `SUBSTRING:0,5` | Extract substring |
| `PAD_LEFT:len,char` | `PAD_LEFT:6,0` | Pad left |
| `ROUND:decimals` | `ROUND:2` | Round number |
| `MASK:show,hide` | `MASK:2,2` | Mask sensitive data |

## Deployment

### Windows EC2 Deployment

1. **Prerequisites**:
   - Windows Server 2019 or later
   - Java 21 installed
   - Oracle client installed

2. **Install as Windows Service**:
```batch
# Using NSSM (Non-Sucking Service Manager)
nssm install IntegrationService "C:\Java\bin\java.exe" "-jar C:\app\integration-service.jar"
nssm set IntegrationService AppDirectory "C:\app"
nssm set IntegrationService DisplayName "Multi-Tenant Integration Service"
nssm start IntegrationService
```

3. **Configure Windows Task Scheduler** for daily reports:
```batch
# Create scheduled task for 9:00 AM IST
schtasks /create /tn "DailyAuditReport" /tr "C:\Java\bin\java.exe -jar C:\app\integration-service.jar --spring.profiles.active=report" /sc daily /st 09:00
```

### Log Location

```
/var/log/integration-service/
├── multi-tenant-integration.log
├── multi-tenant-integration-audit.log
├── multi-tenant-integration-error.log
└── multi-tenant-integration-json.log
```

## Troubleshooting

### Common Issues

1. **Connection timeout to external API**
   - Check `rest.client.timeout.seconds` configuration
   - Verify network connectivity and firewall rules
   - Check client configuration `TIMEOUT_SECONDS`

2. **Audit write failures**
   - Verify database connectivity
   - Check database permissions
   - Monitor transaction deadlocks

3. **Missing mandatory fields**
   - Verify source table has required columns
   - Check field mapping configuration
   - Verify source record exists

4. **Encryption/Decryption errors**
   - Verify `JASYPT_MASTER_PASSWORD` environment variable
   - Verify `AES_SECRET_KEY` length (32 characters)
   - Re-encrypt credentials if master password changed

### Monitoring

Access Spring Boot Actuator endpoints:

```
GET /actuator/health - Health status
GET /actuator/metrics - Application metrics
GET /actuator/info - Application info
```

## License

MIT License

## Support

For issues and feature requests, please open an issue on GitHub.
