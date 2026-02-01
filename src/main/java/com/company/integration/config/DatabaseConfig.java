package com.company.integration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * Database configuration class.
 * Configures transaction management for Oracle database operations.
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    /**
     * Configure platform transaction manager for MyBatis operations.
     *
     * @param dataSource the configured data source
     * @return PlatformTransactionManager instance
     */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
