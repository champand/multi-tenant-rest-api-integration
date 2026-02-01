package com.company.integration.config;

import org.apache.ibatis.session.ExecutorType;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis configuration class.
 * Configures SQL session factory and mapper scanning.
 */
@Configuration
@MapperScan(basePackages = "com.company.integration.mapper")
public class MyBatisConfig {

    /**
     * Configure MyBatis SQL session factory.
     *
     * @param dataSource the configured data source
     * @return SqlSessionFactoryBean instance
     * @throws Exception if configuration fails
     */
    @Bean
    public SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);

        // Configure mapper locations
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactory.setMapperLocations(resolver.getResources("classpath:mapper/*.xml"));

        // Configure type aliases
        sessionFactory.setTypeAliasesPackage("com.company.integration.model.entity");

        // Configure MyBatis settings
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setJdbcTypeForNull(org.apache.ibatis.type.JdbcType.NULL);
        configuration.setDefaultExecutorType(ExecutorType.REUSE);
        configuration.setDefaultStatementTimeout(30);
        configuration.setCacheEnabled(false); // Disable caching as per requirements
        configuration.setLazyLoadingEnabled(false);

        sessionFactory.setConfiguration(configuration);

        return sessionFactory;
    }
}
