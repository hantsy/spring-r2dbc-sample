package com.example.demo;

import io.r2dbc.proxy.ProxyConnectionFactoryProvider;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.proxy.support.QueryExecutionInfoFormatter;
import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.r2dbc.autoconfigure.ConnectionFactoryOptionsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@SpringBootApplication
@Slf4j
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}

@Configuration
@Slf4j
class R2dbcProxyConfig {
	
	// R2dbc URL with pooling
    // spring.r2dbc.url=r2dbc:proxy:pool:postgresql://localhost:5432/myDB?proxyListener=com.example.bootr2dbc.config.QueryProxyExecutionListener&maxIdleTime=PT60S
    @Bean
    ConnectionFactoryOptionsBuilderCustomizer postgresCustomizer(ProxyExecutionListener queryProxyExecutionListener) {
        return builder -> {
            builder.option(ConnectionFactoryOptions.LOCK_WAIT_TIMEOUT, Duration.ofSeconds(30));
            builder.option(ConnectionFactoryOptions.STATEMENT_TIMEOUT, Duration.ofMinutes(1));
            builder.option(ConnectionFactoryOptions.DRIVER, "proxy");
            builder.option(ConnectionFactoryOptions.PROTOCOL, "postgresql");
            builder.option(
                    ProxyConnectionFactoryProvider.PROXY_LISTENERS, queryProxyExecutionListener);
        };
    }

    @Bean
    ProxyExecutionListener queryProxyExecutionListener() {
        return new ProxyExecutionListener(){
            @Override
            public void afterQuery(QueryExecutionInfo queryExecutionInfo) {
                QueryExecutionInfoFormatter formatter = QueryExecutionInfoFormatter.showAll();
                String str = formatter.format(queryExecutionInfo);
                log.info("Query: {}", str);
                log.info("Execution Time: {} ms", queryExecutionInfo.getExecuteDuration().toMillis());
            }
        };
    }
}

interface PostRepository extends R2dbcRepository<Post, UUID> {
}

@Table(value = "posts")
record Post(

        @Id
        @Column("id")
        UUID id,

        @Column("title")
        String title,

        @Column("content")
        String content,

        @Column("status")
        Status status,

        @Column("version")
        @Version
        Long version
) {

    public static Post of(String title, String content) {
        return new Post(null, title, content, null, null);
    }

    enum Status {
        DRAFT, PENDING_MODERATION, PUBLISHED;
    }

}
