package com.example.demo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.api.PostgresqlConnection;
import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication(exclude = {
        R2dbcAutoConfiguration.class,
        R2dbcDataAutoConfiguration.class,
        R2dbcRepositoriesAutoConfiguration.class,
        R2dbcTransactionManagerAutoConfiguration.class
})
@Slf4j
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        return new PostgresqlConnectionFactory(
                PostgresqlConnectionConfiguration.builder()
                        .host("localhost")
                        .database("blogdb")
                        .username("user")
                        .password("password")
                        //.codecRegistrar(EnumCodec.builder().withEnum("post_status", Post.Status.class).build())
                        .build()
        );
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            builder.serializationInclusion(JsonInclude.Include.NON_EMPTY);
            builder.featuresToDisable(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                    SerializationFeature.FAIL_ON_EMPTY_BEANS,
                    DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            builder.featuresToEnable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        };
    }
}

@Component
@Slf4j
@RequiredArgsConstructor
class Notifier {

    private final ConnectionFactory connectionFactory;

    private final ObjectMapper objectMapper;

    PostgresqlConnection sender;

    @PostConstruct
    public void initialize() throws InterruptedException {
        sender = Mono.from(connectionFactory.create())
                .cast(PostgresqlConnection.class)
                .block();
    }

    @SneakyThrows
    public Mono<Void> send(CreateMessageCommand data) {
        var message = new Message(UUID.randomUUID(), data.body(), LocalDateTime.now());
        var messageJson = objectMapper.writeValueAsString(message);

        return sender.createStatement("NOTIFY messages, '" + messageJson + "'")
                .execute()
                .flatMap(PostgresqlResult::getRowsUpdated)
                .log("sending notification::")
                .then();
    }

    @PreDestroy
    public void destroy() {
        sender.close().subscribe();
    }

}

@Component
@Slf4j
@RequiredArgsConstructor
class Listener {

    private final ConnectionFactory connectionFactory;

    private final ObjectMapper objectMapper;

    PostgresqlConnection receiver;

    @PostConstruct
    public void initialize() throws InterruptedException {
        receiver = Mono.from(connectionFactory.create())
                .cast(PostgresqlConnection.class)
                .block();

        receiver.createStatement("LISTEN messages")
                .execute()
                .flatMap(PostgresqlResult::getRowsUpdated)
                .log("listen::")
                .subscribe();
    }

    public Flux<Message> getMessages() {
        return receiver.getNotifications()
                .delayElements(Duration.ofMillis(100))
                .log()
                .map(notification -> {
                    log.debug("received notification: {}", notification);
                    try {
                        return objectMapper.readValue(notification.getParameter(), Message.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @PreDestroy
    public void destroy() {
        receiver.close().subscribe();
    }

}


@Configuration
class WebConfig {

    @Bean
    public RouterFunction<ServerResponse> routes(MessageHandler messageHandler) {
        return route()
                .path("/messages", () -> route()
                                .nest(
                                        path(""),
                                        () -> route()
                                                .GET("", messageHandler::all)
                                                .POST("", messageHandler::create)
                                                .build()
                                )
//                        .nest(
//                                path("{id}"),
//                                () -> route()
//                                        .GET("", messageHandler::get)
//                                        .build()
//                        )
                                .build()
                ).build();
    }
}

@Component
@RequiredArgsConstructor
class MessageHandler {

    private final Notifier notifier;
    private final Listener listener;

    public Mono<ServerResponse> all(ServerRequest req) {
        return ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(this.listener.getMessages(), Message.class);
    }

    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(CreateMessageCommand.class)
                .flatMap(this.notifier::send)
                .flatMap(__ -> ok().bodyValue("The message was sent"));
    }

}

record CreateMessageCommand(String body) {
}

record Message(UUID id, String body, LocalDateTime sentAt) {
}