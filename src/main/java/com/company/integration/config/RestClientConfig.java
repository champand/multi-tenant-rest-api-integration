package com.company.integration.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * REST client configuration class.
 * Configures WebClient for external API calls with proper timeout settings.
 */
@Configuration
public class RestClientConfig {

    @Value("${rest.client.timeout.seconds:300}")
    private int timeoutSeconds;

    @Value("${rest.client.connection.timeout.seconds:30}")
    private int connectionTimeoutSeconds;

    @Value("${rest.client.max.connections:100}")
    private int maxConnections;

    @Value("${rest.client.max.connections.per.route:20}")
    private int maxConnectionsPerRoute;

    /**
     * Configure WebClient for external API calls.
     *
     * @return WebClient.Builder instance
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        // Configure connection provider with pool settings
        ConnectionProvider connectionProvider = ConnectionProvider.builder("integration-pool")
                .maxConnections(maxConnections)
                .maxIdleTime(Duration.ofMinutes(5))
                .maxLifeTime(Duration.ofMinutes(30))
                .pendingAcquireTimeout(Duration.ofSeconds(connectionTimeoutSeconds))
                .evictInBackground(Duration.ofMinutes(2))
                .build();

        // Configure HTTP client with timeouts
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutSeconds * 1000)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS)));

        // Configure exchange strategies for large payloads
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies);
    }

    /**
     * Create default WebClient instance.
     *
     * @param webClientBuilder the configured builder
     * @return WebClient instance
     */
    @Bean
    public WebClient webClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.build();
    }
}
