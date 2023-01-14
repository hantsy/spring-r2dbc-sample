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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.jackson.JsonComponent;
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
class DataR2dbcConfig {
    @Bean
    R2dbcCustomConversions r2dbcCustomConversions(ConnectionFactory factory, ObjectMapper objectMapper) {
        R2dbcDialect dialect = DialectResolver.getDialect(factory);
        return R2dbcCustomConversions
                .of(
                        dialect,
                        List.of(
                                new JsonToStatisticsConverter(objectMapper),
                                new StatisticsToJsonConverter(objectMapper)
                        )
                );
    }

}

@ReadingConverter
@RequiredArgsConstructor
class JsonToStatisticsConverter implements Converter<Json, Post.Statistics> {
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public Post.Statistics convert(Json source) {
        return objectMapper.readValue(source.asString(), Post.Statistics.class);
    }
}

@WritingConverter
@RequiredArgsConstructor
class StatisticsToJsonConverter implements Converter<Post.Statistics, Json> {
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public Json convert(Post.Statistics source) {
        return Json.of(objectMapper.writeValueAsString(source));
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
                            if (p2 != null && StringUtils.hasText(p2.getTitle())) {
                                p.setTitle(p2.getTitle());
                            }

                            if (p2 != null && StringUtils.hasText(p2.getContent())) {
                                p.setContent(p2.getContent());
                            }

                            if (p2 != null && p2.getMetadata() != null) {
                                p.setMetadata(p2.getMetadata());
                            }

                            if (p2 != null && p2.getStatus() != null) {
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
