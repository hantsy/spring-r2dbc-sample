package com.example.demo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.ConnectionFactory;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryOptionsBuilderCustomizer;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider.OPTIONS;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@SpringBootApplication
@Slf4j
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    ApplicationRunner initialize(
            DatabaseClient databaseClient,
            PostRepository posts,
            CommentRepository comments,
            TransactionalOperator operator) {
        log.info("start data initialization...");
        return args -> {
            databaseClient
                    .sql("INSERT INTO  posts (title, content, metadata) VALUES (:title, :content, :metadata)")
                    .filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
                    .bind("title", "my first post")
                    .bind("content", "content of my first post")
                    .bind("metadata", Json.of("{\"tags\":[\"spring\", \"r2dbc\"]}"))
                    .fetch()
                    .first()
                    .subscribe(
                            data -> log.info("inserted data : {}", data),
                            error -> log.error("error: {}", error)
                    );

            posts
                    .save(Post.builder().title("another post").content("content of another post").build())
                    .map(p -> {
                        p.setTitle("new Title");
                        return p;
                    })
                    .flatMap(posts::save)
                    .flatMap(saved -> comments
                            .save(Comment.builder()
                                    .content("dummy comments")
                                    .postId(saved.getId())
                                    .build()
                            )
                    )
                    .log()
                    .then()
                    .thenMany(posts.findAll())
                    .as(operator::transactional)
                    .subscribe(
                            data -> log.info("saved data: {}", data),
                            err -> log.error("err: {}", err)
                    );

        };

    }

    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {

        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
        populator.addPopulators(new ResourceDatabasePopulator(new ClassPathResource("schema.sql")));
        populator.addPopulators(new ResourceDatabasePopulator(new ClassPathResource("data.sql")));
        initializer.setDatabasePopulator(populator);

        return initializer;
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

@Configuration
@EnableR2dbcAuditing
class DataConfig {

    @Bean
    public ConnectionFactoryOptionsBuilderCustomizer postgresCustomizer() {
        Map<String, String> options = new HashMap<>();
        options.put("lock_timeout", "30s");
        options.put("statement_timeout", "60s");
        return (builder) -> builder.option(OPTIONS, options);
    }

    @Bean
    ReactiveAuditorAware<String> auditorAware() {
        return () -> Mono.just("hantsy");
    }
}

@Configuration
class WebConfig {

    @Bean
    public RouterFunction<ServerResponse> routes(PostHandler postController, CommentHandler commentHandler) {
        return route()
                .path("/posts", () -> route()
                        .nest(
                                path(""),
                                () -> route()
                                        .GET("", postController::all)
                                        .POST("", postController::create)
                                        .build()
                        )
                        .nest(
                                path("{id}"),
                                () -> route()
                                        .GET("", postController::get)
                                        .PUT("", postController::update)
                                        .DELETE("", postController::delete)
                                        .nest(
                                                path("comments"),
                                                () -> route()
                                                        .GET("", commentHandler::getByPostId)
                                                        .POST("", commentHandler::create)
                                                        .build()
                                        )
                                        .build()
                        )
                        .build()
                ).build();
    }
}

@Slf4j
@JsonComponent
class PgJsonObjectJsonComponent {

    static class Deserializer extends JsonDeserializer<Json> {

        @Override
        public Json deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var value = ctxt.readTree(p);
            log.info("read json value :{}", value);
            return Json.of(value.toString());
        }
    }

    static class Serializer extends JsonSerializer<Json> {

        @Override
        public void serialize(Json value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            var text = value.asString();
            log.info("The raw json value from PostgresSQL JSON type:{}", text);
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(text);
            var node = gen.getCodec().readTree(parser);
            serializers.defaultSerializeValue(node, gen);
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
                .flatMap(post -> created(URI.create("/posts/" + post.getId())).build());
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
                            p.setTitle(p2.getTitle());
                            p.setContent(p2.getContent());
                            p.setMetadata(p2.getMetadata());
                            p.setStatus(p2.getStatus());
                            return p;
                        },
                        existed,
                        req.bodyToMono(Post.class)
                )
                .cast(Post.class)
                .flatMap(this.posts::save)
                .flatMap(post -> noContent().build());
    }

    public Mono<ServerResponse> delete(ServerRequest req) {
        return this.posts.deleteById(UUID.fromString(req.pathVariable("id")))
                .flatMap(deleted -> noContent().build());
    }
}

interface PostRepository extends R2dbcRepository<Post, UUID> {

    @Query("SELECT * FROM posts where title like :title")
    public Flux<Post> findByTitleContains(String title);

    public Flux<PostSummary> findByTitleLike(String title, Pageable pageable);
}

@Value
class PostSummary {
    UUID id;
    String title;
}

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "posts")
class Post {

    @Id
    @Column("id")
    private UUID id;

    @Column("title")
    private String title;

    @Column("content")
    private String content;

    @Column("metadata")
    private Json metadata;

    @Column("status")
    private Status status;

    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column("version")
    @Version
    private Long version;

    enum Status {
        DRAFT, PENDING_MODERATION, PUBLISHED;
    }

}

@Component
class CommentHandler {

    private final CommentRepository comments;

    public CommentHandler(CommentRepository comments) {
        this.comments = comments;
    }

    public Mono<ServerResponse> create(ServerRequest req) {
        var postId = UUID.fromString(req.pathVariable("id"));
        return req.bodyToMono(Comment.class)
                .map(comment -> {
                    comment.setPostId(postId);
                    return comment;
                })
                .flatMap(this.comments::save)
                .flatMap(c -> created(URI.create("/posts/" + postId + "/comments/" + c.getId())).build());
    }

    public Mono<ServerResponse> getByPostId(ServerRequest req) {
        var result = this.comments.findByPostId(UUID.fromString(req.pathVariable("id")));
        return ok().body(result, Comment.class);
    }
}

interface CommentRepository extends R2dbcRepository<Comment, UUID> {
    Flux<Comment> findByPostId(UUID postId);
}

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "comments")
class Comment {

    @Id
    @Column("id")
    private UUID id;

    @Column("content")
    private String content;

    @Column("post_id")
    private UUID postId;

    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column("version")
    @Version
    private Long version;

}

