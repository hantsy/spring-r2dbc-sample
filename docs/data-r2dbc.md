# *Update*: Accessing RDBMS with Spring Data R2dbc

I have written [an article to introduce Spring Data R2dbc](https://medium.com/@hantsy/reactive-accessing-rdbms-with-spring-data-r2dbc-d6e453f2837e) before, this article is an update to the latest Spring 5.3 and Spring Data R2dbc 1.2.



## Add Spring Data R2dbc Dependency

Add the following dependency into your project dependencies.

```xml
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-r2dbc</artifactId>
</dependency>
```

For Spring Boot applications, add the Spring Boot starter for Spring Data R2dbc.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
```

## Configuring Spring Data R2dbc

Beside the R2dbc drivers, when using Spring Data R2dbc, add the following dependencies into your project.

Spring Data R2dbc provides a templated `AbstractR2dbcConfiguration` class to simplify the configuration, override the `connectionFactory()` to provide a `ConnectionFactory` bean. You can also register the custom converters used to convert between data type and Java type.

```java
@Configuration
@EnableTransactionManagement
public class DatabaseConfig extends AbstractR2dbcConfiguration {

    @Override
    @Bean
    public ConnectionFactory connectionFactory() {
        // postgres
        return new PostgresqlConnectionFactory(
                PostgresqlConnectionConfiguration.builder()
                        .host("localhost")
                        .database("test")
                        .username("user")
                        .password("password")
                        .codecRegistrar(EnumCodec.builder().withEnum("post_status", Post.Status.class).build())
                        .build()
        );
    }

    @Override
    protected List<Object> getCustomConverters() {
        return List.of(new PostReadingConverter(), new PostStatusWritingConverter());
    }

    @Bean
    ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
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

}

```
In above codes,  a `@EnableTransactionManagement` is added to the configuration to activate the reactive transaction management support, to use `@Transactional` annotation, you have to declare a `ReactiveTransactionManager` bean. Spring provides a `R2dbcTransactionManager`. As described in the former post, a `ConnectionFactoryInitializer` bean is declared to initialize the table schema and sample data.

In Spring Boot applications, simply configure the **spring.r2dbc.url**, **spring.r2dbc.username**, **spring.r2dbc.password** properties, Spring boot will autoconfigure these for you. Of course you can customize your own configuration by subclassing `AbstractR2dbcConfiguration`, check [my example config fragment](https://github.com/hantsy/spring-r2dbc-sample/blob/master/boot/src/main/java/com/example/demo/DemoApplication.java#L236-L261).

Next, we can use Spring Data R2dbc's specific `EntityTemplate` or `R2dbcRepository`  to perform CRUD operations on databases. 

## EntityTemplate

The `EntityTemplate` is a lightweight wrapper of `DatabaseClient` , but it provides type safe operations and fluent query APIs instead of literal SQL queries.

A `EntityTemplate` bean is declared in the  `AbstractR2dbcConfiguration` class.  So  you can inject it directly.

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PostRepository {

    private final R2dbcEntityTemplate template;

    public Flux<Post> findByTitleContains(String name) {
        return this.template.select(Post.class)
                .matching(Query.query(where("title").like("%" + name + "%")).limit(10).offset(0))
                .all();
    }

    public Flux<Post> findAll() {
        return this.template.select(Post.class).all();
    }

    public Mono<Post> findById(UUID id) {
        return this.template.selectOne(Query.query(where("id").is(id)), Post.class);
    }

    public Mono<UUID> save(Post p) {
        return this.template.insert(Post.class)
                .using(p)
                .map(post -> post.getId());
    }

    public Mono<Integer> update(Post p) {
/*
        return this.template.update(Post.class)
                .matching(Query.query(where("id").is(p.getId())))
                .apply(Update.update("title", p.getTitle())
                        .set("content", p.getContent())
                        .set("status", p.getStatus())
                        .set("metadata", p.getMetadata()));
*/
        return this.template.update(
                Query.query(where("id").is(p.getId())),
                Update.update("title", p.getTitle())
                        .set("content", p.getContent())
                Post.class
        );
    }

    public Mono<Integer> deleteById(UUID id) {
        return this.template.delete(Query.query(where("id").is(id)), Post.class);
    }
}
```

Let's have a look at the `Post` class.

```java
@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "posts")
public class Post {

    @Id
    @Column("id")
    private UUID id;

    @Column("title")
    private String title;

    @Column("content")
    private String content;

}

```

The `Post` class is annotated with a `@Table` annotation which accepts the mapped table name. `@Id` specifies its the identifier of this entity, `@Column`  defines the column name in the table.

> Please note, `@Table`  and  `@Column` is from the spring data relational, which is common for Spring Data Jdbc and Spring Data R2dbc, and `@Id` is from Spring Data Commons. 

## R2dbcRepository

To enable `Repository` support, add a `@EnableR2dbcRepositories` annotation on the configuration class.

```java
@Configuration
@EnableR2dbcRepositories
class DatabaseConfig{}
```

If the entity classes are not in the same package or subpackages of the config class, you have to set the `basePackages` attribute to locate the entities.

In the Spring Boot applications, `@EnableR2dbcRepositories` is not a must.

A simple `Repository` looks like the following.

```java
public interface PostRepository extends R2dbcRepository<Post, UUID> {
    public Flux<Post> findByTitleContains(String name);
}
```

It also supports some common Spring data features, such as `@Query` annotations on methods and named or index-based parameters.

