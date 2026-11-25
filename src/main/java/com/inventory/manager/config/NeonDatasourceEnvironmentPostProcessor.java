package com.inventory.manager.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Translates Neon's libpq-style {@code DATABASE_URL} (pooled) and {@code DATABASE_URL_UNPOOLED}
 * (direct) into Spring Boot JDBC and Flyway properties when not running under the {@code test} profile.
 */
public class NeonDatasourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.acceptsProfiles(Profiles.of("test"))) {
            return;
        }

        Map<String, Object> props = new HashMap<>();

        String pooledUrl = environment.getProperty("DATABASE_URL");
        if (pooledUrl != null && !pooledUrl.isBlank()) {
            JdbcParts jdbc = parsePostgresUri(pooledUrl);
            props.put("spring.datasource.url", jdbc.jdbcUrl());
            props.put("spring.datasource.username", jdbc.username());
            props.put("spring.datasource.password", jdbc.password());
        }

        String unpooledUrl = environment.getProperty("DATABASE_URL_UNPOOLED");
        if (unpooledUrl != null && !unpooledUrl.isBlank()) {
            JdbcParts flywayJdbc = parsePostgresUri(unpooledUrl);
            props.put("spring.flyway.url", flywayJdbc.jdbcUrl());
            props.put("spring.flyway.user", flywayJdbc.username());
            props.put("spring.flyway.password", flywayJdbc.password());
        }

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("neonDatasourceProperties", props));
        }
    }

    static JdbcParts parsePostgresUri(String rawUri) {
        String trimmed = rawUri.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        URI uri = URI.create(trimmed);
        String userInfo = uri.getRawUserInfo();
        String username = "";
        String password = "";
        if (userInfo != null && !userInfo.isEmpty()) {
            String[] parts = userInfo.split(":", 2);
            username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            if (parts.length > 1) {
                password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (uri.getPort() != -1) {
            jdbcUrl.append(":").append(uri.getPort());
        }
        jdbcUrl.append(uri.getRawPath());
        if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
            jdbcUrl.append("?").append(uri.getRawQuery());
        }
        return new JdbcParts(jdbcUrl.toString(), username, password);
    }

    record JdbcParts(String jdbcUrl, String username, String password) {
    }
}
