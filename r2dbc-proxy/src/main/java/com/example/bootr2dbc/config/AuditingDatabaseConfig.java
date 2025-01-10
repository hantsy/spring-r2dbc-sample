package com.example.bootr2dbc.config;

import io.r2dbc.proxy.ProxyConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactoryOptions;
import java.time.Duration;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryOptionsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;

@Configuration(proxyBeanMethods = false)
@EnableR2dbcAuditing
public class AuditingDatabaseConfig {

    @Bean
    ReactiveAuditorAware<String> auditorAware() {
        return () ->
                ReactiveSecurityContextHolder.getContext()
                        .map(SecurityContext::getAuthentication)
                        .filter(Authentication::isAuthenticated)
                        .map(Authentication::getPrincipal)
                        .map(User.class::cast)
                        .map(User::getUsername);
    }

    @Bean
    ConnectionFactoryOptionsBuilderCustomizer postgresCustomizer(
            QueryProxyExecutionListener queryProxyExecutionListener) {
        return builder -> {
            builder.option(ConnectionFactoryOptions.LOCK_WAIT_TIMEOUT, Duration.ofSeconds(30));
            builder.option(ConnectionFactoryOptions.STATEMENT_TIMEOUT, Duration.ofMinutes(1));
            builder.option(ConnectionFactoryOptions.DRIVER, "proxy");
            builder.option(ConnectionFactoryOptions.PROTOCOL, "postgresql");
            builder.option(
                    ProxyConnectionFactoryProvider.PROXY_LISTENERS, queryProxyExecutionListener);
        };
    }
}
