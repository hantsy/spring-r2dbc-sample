package com.example.demo;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication()
@Slf4j
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}

@Component
@Slf4j
@RequiredArgsConstructor
class Notifier {

    private final ConnectionFactory connectionFactory;

    private final JsonMapper jsonMapper;

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
        var messageJson = jsonMapper.writeValueAsString(message);

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

    private final JsonMapper jsonMapper;

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
                        return jsonMapper.readValue(notification.getParameter(), Message.class);
                    } catch (JacksonException e) {
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