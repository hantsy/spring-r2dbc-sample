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
import org.springframework.data.annotation.*;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.http.HttpMethod;
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@SpringBootApplication
@Slf4j
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    ApplicationRunner initialize(DatabaseClient databaseClient) {
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
        return () -> ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .map(User.class::cast)
                .map(User::getUsername);
    }
}

@Configuration
@Slf4j
class SecurityConfig {

    @Bean
    SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) {
        var POST_PATH = "/posts/**";
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(httpBasicSpec -> httpBasicSpec
                        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                )
                .authorizeExchange(it ->
                        it.pathMatchers(HttpMethod.GET, "/", POST_PATH).permitAll()
                                .pathMatchers(HttpMethod.DELETE, POST_PATH).hasRole("ADMIN")
                                .pathMatchers(POST_PATH).hasRole("USER")
                                .pathMatchers("/users/{user}/**").access(this::currentUserMatchesPath)
                                .anyExchange().authenticated()
                )
                .build();
    }

    private Mono<AuthorizationDecision> currentUserMatchesPath(Mono<Authentication> authentication, AuthorizationContext context) {
        return authentication
                .map(a -> context.getVariables().get("user").equals(a.getName()))
                .map(AuthorizationDecision::new);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.withUsername("user")
                .passwordEncoder(passwordEncoder::encode)
                .password("password")
                .roles("USER")
                .build();
        UserDetails admin = User.withUsername("admin")
                .passwordEncoder(passwordEncoder::encode)
                .password("password")
                .roles("USER", "ADMIN")
                .build();
        log.info("user: {}", user);
        log.info("admin: {}", admin);
        return new MapReactiveUserDetailsService(user, admin);
    }

}

@Configuration
class WebConfig {

    @Bean
    public RouterFunction<ServerResponse> routes(PostHandler postHandler, ReactiveUserDetailsService userDetailsService) {

        var postRoutes = route()
                .GET("", postHandler::all)
                .POST("", postHandler::create)
                .GET("{id}", postHandler::get)
                .PUT("{id}", postHandler::update)
                .DELETE("{id}", postHandler::delete)
                .build();
        return route()
                .path("/posts", () -> postRoutes)
                .GET("/users/{user}", req -> ok().body(userDetailsService.findByUsername(req.pathVariable("user")), UserDetails.class))
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

    @Column("status")
    private Status status;

    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column("created_by")
    @CreatedBy
    private String createdBy;

    @Column("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column("updated_by")
    @LastModifiedBy
    private String updatedBy;

    @Column("version")
    @Version
    private Long version;

    enum Status {
        DRAFT, PENDING_MODERATION, PUBLISHED;
    }

}
