# Persisting Binary Data into Postgres using Spring Data R2dbc

From the [Postgres R2dbc homepage](https://github.com/pgjdbc/r2dbc-postgresql), a Postgres `bytea` data type can be mapped to Java `ByteBuffer`, `byte[]`, and R2dbc specific `Blob`.  In this post we will demonstrate persisting Java types(`ByteBuffer`, `byte[]`, `Blob`, etc.) into Postgres data type `bytea`(byte array).

> Before Spring Data Relational 3.1, there is a bug in Spring Data R2dbc, you have to use the custom R2dbc converters to convert between the Postgres data types and Java types, more details please check the issue [spring-data-relational#1408](https://github.com/spring-projects/spring-data-relational/issues/1408).

Create a Spring Boot project from https://start.spring.io. 
* Add dependencies: Spring Web Reactive, Postgres, Spring Data R2dbc, Testcontainers, Lombok, etc.
* Java version: 21
* Build tools: Maven 

Create an entity class `Post`.

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

    @Column("attachment")
    private ByteBuffer attachment;

    @Column("cover_image")
    private byte[] coverImage;

    @Column("cover_image_thumbnail")
    private Blob coverImageThumbnail;

}
```

In the above entity class, to demonstrate 3 Java types, we create 3 properties, `attachment`, `coverImage`, `coverImageThumbnail` and use `ByteBuffer`, `byte[]`, `Blob` respectively.

Unlike JPA, R2dbc/Spring Data R2dbc does not support initialize the database schemas from entity classes.  Create a `schema.sql` in the *src/main/resources* folder like the following.

```sql
CREATE TABLE IF NOT EXISTS posts (
    -- id SERIAL PRIMARY KEY,
    id UUID DEFAULT uuid_generate_v4(),
    title VARCHAR(255),
    content VARCHAR(255),
    metadata JSON default '{}',
    -- In this sample, use Varchar to store enum(name), Spring Data R2dbc can convert Java Enum to pg VARCHAR, and reverse.
    status VARCHAR(255) default 'DRAFT',
    created_at TIMESTAMP , --NOT NULL DEFAULT LOCALTIMESTAMP,
    updated_at TIMESTAMP,
    attachment bytea,
    cover_image bytea,
    cover_image_thumbnail bytea,
    version INTEGER,
    PRIMARY KEY (id)
);
```

Optionally, create a `data.sql` in the same folder to insert some sample data. The `data.sql` will be executed after `schema.sql`.

In the `application.properties` file, add the following config to ensure the scripts are always executed when the application starts up.

```properties
# embedded, always
spring.sql.init.mode=always 
```

Create a `Repository` for the entity `Post`.

```java
interface PostRepository extends R2dbcRepository<Post, UUID> {
}
```

Let's create a test named `PostRepositoryTest` to test `PostRepository` with a real Postgres database running in Testcontainers.

```java
@DataR2dbcTest()
@Testcontainers
@Slf4j
public class PostRepositoryTest {

    @Container
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer<>("postgres:12")
            .withCopyFileToContainer(MountableFile.forClasspathResource("init.sql"), "/docker-entrypoint-initdb.d/init.sql");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://"
                + postgreSQLContainer.getHost() + ":" + postgreSQLContainer.getFirstMappedPort()
                + "/" + postgreSQLContainer.getDatabaseName());
        registry.add("spring.r2dbc.username", () -> postgreSQLContainer.getUsername());
        registry.add("spring.r2dbc.password", () -> postgreSQLContainer.getPassword());
    }

    @Autowired
    R2dbcEntityTemplate template;

    @Autowired
    PostRepository posts;

    @BeforeEach
    public void setup() {

    }

    @Test
    public void testDatabaseClientExisted() {
        assertNotNull(template);
    }

    @Test
    public void testPostRepositoryExisted() {
        assertNotNull(posts);
    }

}
```

In the above codes, the `@DataR2dbcTest` will autoconfigure the *smallest* test context for testing database using Spring Data R2dbc APIs, when the test environment is ready, you can inject `PostRepository` bean and R2dbc related `DatabaseClient` and `R2dbcEntityTemplate` in the tests directly. The `@Testcontainers` and `@Container` will serve the running services before running tests. The `registerDynamicProperties` static method will configure the R2dbc properties for connecting to the Postgres running in the Testcontainers.

Let's test saving `ByteBuffer` type into database. Create a `ByteBuffer` from text based bytes, and verify the saved Post property `attachment` is equivalent to the input data.

```java
@Test
public void testByteBuffer() {
    String s = "testByteBuffer";
    var post = Post.builder().title("r2dbc").attachment(ByteBuffer.wrap(s.getBytes())).build();
    posts.save(post)
            .as(StepVerifier::create)
            .consumeNextWith(saved -> {
                        assertThat(saved.getTitle()).isEqualTo("r2dbc");
                        var attachment = new String(saved.getAttachment().array());
                        assertThat(attachment).isEqualTo(s);
                    }
            )
            .verifyComplete();
}
```
Similarly, create a new test to test persisting `byte[]` into the database.

```java
@Test
public void testByteArray() {
    String s = "testByteArray";
    var post = Post.builder().title("r2dbc").coverImage(s.getBytes()).build();
    posts.save(post)
            .as(StepVerifier::create)
            .consumeNextWith(saved -> {
                        assertThat(saved.getTitle()).isEqualTo("r2dbc");
                        var attachment = new String(saved.getCoverImage());
                        assertThat(attachment).isEqualTo(s);
                    }
            )
            .verifyComplete();
}
```

Next, let's verify the persistence using the R2dbc specific `Blob` as Java type.

```java
@Test
public void testBlob() {
    String s = "testBlob";
    var post = Post.builder().title("r2dbc").coverImageThumbnail(Blob.from(Mono.just(ByteBuffer.wrap(s.getBytes())))).build();
    posts.save(post)
            .as(StepVerifier::create)
            .consumeNextWith(saved -> {
                        assertThat(saved.getTitle()).isEqualTo("r2dbc");
                        var latch = new CountDownLatch(1);
                        Mono.from(saved.getCoverImageThumbnail().stream())
                                .map(it -> new String(it.array()))
                                .subscribe(it -> {
                                    assertThat(it).isEqualTo(s);
                                    latch.countDown();
                                });

                        try {
                            latch.await(1000, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
            )
            .verifyComplete();
}
```

Check the [sample codes](https://github.com/hantsy/spring-r2dbc-sample/tree/master/boot-filepart) from my Github.

