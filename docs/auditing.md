# Data Auditing with Spring Data R2dbc

For most of Spring developers, I think you are familiar with the simple auditing features in Spring Data project, but in the past years, it only works with the blocking APIs. The long-awaited [Reactive AuditorAware support](https://jira.spring.io/browse/DATACMNS-1379) will be available in the new Spring Data release train.

Let's create a new Spring Boot project to experience the auditing feature.

Open your browser and navigate to [Spring Intializr](https://start.spring.io) page.  

* Build tools: choose Maven as build tools 
* Java version, choose the latest Java 11 or above
* Spring boot version: choose Spring Boot 2.4.0-M3 to get the newest `ReactiveAuditorAware` support
* And add the following dependencies to the project.
  * Reactive Web
  * Spring Data R2dbc
  * Security
  * Lombok

## Enabling Auditing Support

Add `@EnableR2dbcAuditing` annotation on the configuration class.

```java
@Configuration
@EnableR2dbcAuditing
class DatabaseConfig{
    
}
```

Declare a `ReactiveAuditorAware` bean. When a `ReactiveAuditorAware` bean is available, it will fill the fields annotated by `@CreatedBy` and `@LastModifiedBy` annotations automatically in the entity classes. 

```java
@Bean
ReactiveAuditorAware<String> auditorAware() {
    return () -> ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .filter(Authentication::isAuthenticated)
        .map(Authentication::getPrincipal)
        .map(User.class::cast)
        .map(User::getUsername);
}
```

In the above example, we will read the username from Spring `SecurityContext`.  We will introduce the Spring Security configuration later.

## Creating entity classes

Create a simple POJO classes. 

```java
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

}
```

The above class is similar to the entity classes we created in the previous posts, but we add four fields in the class.

* `CreatedBy` will be filled when entity data is persisted and user is authenticated.
* `CreatedDate` will be filled with the timestamp when the entity data is persisted.
* `LastModifiedBy` will be filled when the entity data is updated and user is authenticated.
* `LastModifiedDate` will be filled with the timestamp when the entity is updated.

## Create a Repository for the Entity class

Create a new interface extends the `R2dbcRepository`.

```java
interface PostRepository extends R2dbcRepository<Post, UUID> {

    @Query("SELECT * FROM posts where title like :title")
    public Flux<Post> findByTitleContains(String title);
}
```

In the above example, we used a custom `@Query` to execute a *select* query.

## Exposing RESTful APIs

Define a `RouterFunction` bean to register the routing mapping rules for handlers instead of traditional *controllers*.

```java
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
```

Let's explore the codes of `PostHandler`.

```java
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
```

We will introduce `ReactiveUserDetailsService` soon.

## Securing APIs with Spring Security

When *spring-boot-starter-security* is found in the classpath, Spring Security will be configured automatically.  By default, all paths are protected,  and at the application startup it will generate a user which username is `user` and password is a random string. 

To customize the Security configuration, defines a `SecurityWebFilterChain`.

```java
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
```

In the above codes, we allow unauthenticated users to perform a GET request on path */* or */posts*, it only allows a `ADMIN` role based user to delete a post,  an authenticated user with `USER` role  is allowed  to create and update posts.

And as an example, only the current user can access */users/{user}*.

We define two roles in the above  configuration, let's create two users for test purpose.

```java
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
```

 In the above codes, `PasswordEncoder` is use for password hashing, and here we used an in-memory `Map` to serve a `ReactiveUserDetailsService`. In a real world application, you can implements your own `ReactiveUserDetailsService` interface and fetch users from databases.

## Startup the application

Mentioned in the previous posts, you need to a `ConnectionFactoryInitializer` to initialize the database schema if they are not ready at the application startup.

```java
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
```

To insert some sample data, when using Spring Boot, you can define a `ApplicationRunner` or `CommandLineRunner` bean instead of listening the `ContextRefreshEvent` or `ApplicationReadyEvent`. 

```java
@Bean
ApplicationRunner initialize(DatabaseClient databaseClient) {
    log.info("start data initialization...");
    return args -> {
        databaseClient
            .sql("INSERT INTO  posts (title, content) VALUES (:title, :content)")
            .filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
            .bind("title", "my first post")
            .bind("content", "content of my first post")
            .fetch()
            .first()
            .subscribe(
            data -> log.info("inserted data : {}", data),
            error -> log.error("error: {}", error)
        );

    };

}
```

> Please note, the `DatabaseClient` dose not trigger the auditing events, when using `R2dbcEntityTempplate` or `R2dbcRepository` , both work well.

Grab the [sample codes](https://github.com/hantsy/spring-r2dbc-sample/) from my Github.


