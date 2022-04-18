package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryOptionsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider.OPTIONS;

@Configuration
@EnableR2dbcAuditing
class R2dbcConfig {

    @Bean
    public ConnectionFactoryOptionsBuilderCustomizer postgresCustomizer() {
        Map<String, String> options = new HashMap<>();
        options.put("lock_timeout", "30s");
        options.put("statement_timeout", "60s");
        return (builder) -> builder.option(OPTIONS, options);
    }

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        var dialect = DialectResolver.getDialect(connectionFactory);
        var converters = List.of(
                new MapToJsonConverter(objectMapper),
                new JsonToMapConverter(objectMapper),
                new AuthorReadConverter(),
                new AuthorWriteConverter()
        );
        return R2dbcCustomConversions.of(dialect, converters);
    }

    @Bean
    ReactiveAuditorAware<String> auditorAware() {
        return () -> Mono.just("hantsy");
    }
}
