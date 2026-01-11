package com.example.demo;


import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;
import org.springframework.data.r2dbc.mapping.event.AfterSaveCallback;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.r2dbc.mapping.event.BeforeSaveCallback;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@SpringBootApplication
@Slf4j
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}

@Configuration
class WebConfig {

    @Bean
    public RouterFunction<ServerResponse> routes(
            PostLogRepository logs,
            PostHandler postHandler) {

        var postRoutes = route()
                .GET("", postHandler::all)
                .POST("", postHandler::create)
                .GET("{id}", postHandler::get)
                .PUT("{id}", postHandler::update)
                .DELETE("{id}", postHandler::delete)
                .build();
        return route()
                .GET("/logs", req -> ServerResponse.ok().body(logs.findAll(), PostLog.class))
                .path("/posts", () -> postRoutes)
                .build();
    }
}

@Slf4j
@JacksonComponent
class PgJsonObjectJsonComponent {

    static class Deserializer extends ValueDeserializer<Json> {

        @Override
        public Json deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            var value = ctxt.readTree(p);
            log.info("read json value :{}", value);
            return Json.of(value.toString());
        }
    }

    static class Serializer extends ValueSerializer<Json> {

        @Override
        public void serialize(Json value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            String jsonString = value.toString();
            log.debug("serializing JSON value: {}", jsonString);
            ctxt.writeValue(gen, jsonString);
        }

    }
}


@Component
class PostHandler {

    private final PostRepository posts;

    public PostHandler(PostRepository posts) {
        this.posts = posts;
    }

    public Mono<ServerResponse> all(ServerRequest req) {
        return ok().body(this.posts.findAll(), Post.class);
    }

    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(Post.class)
                .flatMap(this.posts::save)
                .flatMap(post -> created(URI.create("/posts/" + post.id())).build());
    }

    public Mono<ServerResponse> get(ServerRequest req) {
        return this.posts.findById(UUID.fromString(req.pathVariable("id")))
                .flatMap(post -> ok().body(Mono.just(post), Post.class))
                .switchIfEmpty(notFound().build());
    }

    public Mono<ServerResponse> update(ServerRequest req) {
        var existed = this.posts.findById(UUID.fromString(req.pathVariable("id")));
        return Mono
                .zip(
                        (data) -> {
                            Post p = (Post) data[0];
                            Post p2 = (Post) data[1];
                            return new Post(
                                    p.id(),
                                    p2.title(),
                                    p2.content(),
                                    p2.status() != null ? p2.status() : p.status(),
                                    p.version()
                            );
                        },
                        existed,
                        req.bodyToMono(Post.class)
                )
                .cast(Post.class)
                .flatMap(this.posts::save)
                .flatMap(post -> noContent().build())
                .switchIfEmpty(notFound().build());
    }

    public Mono<ServerResponse> delete(ServerRequest req) {
        return this.posts.findById(UUID.fromString(req.pathVariable("id")))
                .flatMap(this.posts::delete)
                .flatMap(deleted -> noContent().build())
                .switchIfEmpty(notFound().build());
    }
}

@Configuration
@EnableR2dbcAuditing
class DataR2dbcConfig {

    @Bean
    ReactiveAuditorAware<String> reactiveAuditorAware() {
        return () -> Mono.just("test");
    }
}

interface PostRepository extends R2dbcRepository<Post, UUID> {

    @Query("SELECT * FROM posts where title like :title")
    public Flux<Post> findByTitleContains(String title);
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
    enum Status {
        DRAFT, PENDING_MODERATION, PUBLISHED;
    }

    static Post of(String title, String content) {
        return new Post(null, title, content, Status.DRAFT, null);
    }

}

// save a snapshot into a auditing log table.
@Component
@RequiredArgsConstructor
@Slf4j
class PostCallback implements
        BeforeConvertCallback<Post>,
        AfterConvertCallback<Post>,
        BeforeSaveCallback<Post>,
        AfterSaveCallback<Post> {
    final ApplicationEventPublisher publisher;
    final JsonMapper jsonMapper;

    @SneakyThrows
    @Override
    public Publisher<Post> onAfterSave(Post entity, OutboundRow outboundRow, SqlIdentifier table) {
        log.info("[onAfterSave]::: entity :{}, outboundRow :{}, table: {}", entity, outboundRow, table);
        var entityLog = PostLog.of(entity.id(), entity.getClass().getName(), Json.of(jsonMapper.writeValueAsString(entity)));
        this.publisher.publishEvent(entityLog);
        return Mono.just(entity);

    }

    @Override
    public Publisher<Post> onAfterConvert(Post entity, SqlIdentifier table) {
        log.info("[onAfterConvert]::: entity :{}, table: {}", entity, table);
        return Mono.just(entity);
    }

    @Override
    public Publisher<Post> onBeforeConvert(Post entity, SqlIdentifier table) {
        log.info("[onBeforeConvert]::: entity :{}, table: {}", entity, table);
        return Mono.just(entity);
    }

    @Override
    public Publisher<Post> onBeforeSave(Post entity, OutboundRow row, SqlIdentifier table) {
        log.info("[onBeforeSave]::: entity :{}, outboundRow :{}, table: {}", entity, row, table);
        return Mono.just(entity);
    }
}

@Component
@RequiredArgsConstructor
@Slf4j
class LogEventListener {
    final PostLogRepository logs;

    @EventListener
    public void saveLog(PostLog postLog) {
        log.info("[LogEventListener] saving log: {}", postLog);
        logs.save(postLog).subscribe();
    }
}


interface PostLogRepository extends R2dbcRepository<PostLog, UUID> {

}

@Table("post_logs")
record PostLog(
        @Id
        String id,

        @Column("entity_id")
        UUID entityId,

        @Column("entity_type")
        String entityType,

        @Column("snapshot")
        Json snapshot,

        @CreatedDate
        @Column("created_at")
        LocalDateTime createdAt,

        @CreatedBy
        @Column("created_by")
        String createdBy,

        @Version
        Long version
) {
    static PostLog of(UUID entityId, String entityType, Json json) {
        return new PostLog(null, entityId, entityType, json, null, null, null);
    }
}
