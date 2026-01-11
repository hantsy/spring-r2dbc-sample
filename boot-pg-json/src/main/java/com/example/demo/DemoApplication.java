package com.example.demo;


import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.ConnectionFactory;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.util.List;
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
class DataR2dbcConfig {
    @Bean
    R2dbcCustomConversions r2dbcCustomConversions(ConnectionFactory factory, JsonMapper jsonMapper) {
        R2dbcDialect dialect = DialectResolver.getDialect(factory);
        return R2dbcCustomConversions
                .of(
                        dialect,
                        List.of(
                                new JsonToStatisticsConverter(jsonMapper),
                                new StatisticsToJsonConverter(jsonMapper)
                        )
                );
    }

}

@ReadingConverter
@RequiredArgsConstructor
class JsonToStatisticsConverter implements Converter<Json, Post.Statistics> {
    private final JsonMapper jsonMapper;

    @SneakyThrows
    @Override
    public Post.Statistics convert(Json source) {
        return jsonMapper.readValue(source.asString(), Post.Statistics.class);
    }
}

@WritingConverter
@RequiredArgsConstructor
class StatisticsToJsonConverter implements Converter<Post.Statistics, Json> {
    private final JsonMapper jsonMapper;

    @SneakyThrows
    @Override
    public Json convert(Post.Statistics source) {
        return Json.of(jsonMapper.writeValueAsString(source));
    }
}


@Configuration
class WebConfig {

    @Bean
    public RouterFunction<ServerResponse> routes(PostHandler postHandler) {

        var postRoutes = route()
                .GET("", postHandler::all)
                .POST("", postHandler::create)
                .GET("{id}", postHandler::get)
                .PUT("{id}", postHandler::update)
                .DELETE("{id}", postHandler::delete)
                .build();
        return route()
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
            var text = value.asString();
            log.info("The raw json value from PostgresSQL JSON type:{}", text);
            ctxt.writeValue(gen, text);
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
                            if (StringUtils.hasText(p2.getTitle())) {
                                p.setTitle(p2.getTitle());
                            }

                            if (StringUtils.hasText(p2.getContent())) {
                                p.setContent(p2.getContent());
                            }

                            if (p2.getMetadata() != null) {
                                p.setMetadata(p2.getMetadata());
                            }

                            if (p2.getStatus() != null) {
                                p.setStatus(p2.getStatus());
                            }
                            return p;
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

interface PostRepository extends R2dbcRepository<Post, UUID> {

    @Query("SELECT * FROM posts where title like :title")
    public Flux<Post> findByTitleContains(String title);
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

    @Column("statistics")
    private Statistics statistics;

    @Column("status")
    private Status status;

    @Column("version")
    @Version
    private Long version;

    record Statistics(
            Integer viewed,
            Integer bookmarked
    ) {
    }

    enum Status {
        DRAFT, PENDING_MODERATION, PUBLISHED;
    }

}
