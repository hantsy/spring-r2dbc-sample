# Dealing with Postgres specific Json/Enum type and Notifier/Listener with R2dbc

In the previous posts, we have explored the R2dbc support for H2, MySQL, Postgres and MSSQL, but every database has its own specific functionality, for example, Postgres drivers for R2dbc has built-in  support of  JSON and Enum type. In this post, we will focus on these specific features provided in Postgres.

## JSON type 

Postgres support two forms of JSON types.

* **json** -  storing data as textual form in databases
* **jsonb** - storing data as binary form in the databases

For example, add a *metadata* column to the *posts* table we created in the previous posts.

```sql
CREATE TABLE IF NOT EXISTS posts (
-- other column definitions are omiited
   metadata json default '{}',
   PRIMARY KEY (id)
 );
```

The json data type can be converted to Java `io.r2dbc.postgresql.codec.Json`  and  reverse.

```java
class Post {
    //other properties are omiited
    @Column("metadata")// use for Spring Data R2dbc
    private Json metadata;
}
```

If you are using `DatabaseClient` to insert data  into tables, bind a value of the `Json` type to the json data type, eg.

```java
this.databaseClient
    .sql("INSERT INTO  posts (title, content, metadata) VALUES (:title, :content, :metadata)")
    .filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
    .bind("title", "my first post")
    .bind("content", "content of my first post")
    .bind("metadata", Json.of("{\"tags\":[\"spring\", \"r2dbc\"]}"))
    .fetch()
    .first()
    .subscribe(
        data -> log.info("inserted data : {}", data),
        error -> log.info("error: {}", error)
    );
```

When retrieving data from databases, you need to convert the json data type to Java `Json` type.

 ```java
// code fragments of row result mapping
row.get("metadata", Json.class)
 ```

If you are using Spring Data R2dbc APIs to operate the databases, either `R2dbcEntityTemplate` or `Repository` interface, it will do the conversion work automatically.

```java
// query by id
this.template.selectOne(Query.query(where("id").is(id)), Post.class);

// save 
this.template.insert(Post.class)
    .using(p)
    .map(post -> post.getId());
```

In a  WebMVC/WebFlux application, if you wan to expose RESTful APIs and  use the entity class(eg. `Post` class here) as the  payload of the request or response body, you need to customize the serialization or deserialization of `Json` class.

```java
@Slf4j
public class PgJsonObjectSerializer extends JsonSerializer<Json> {

    @Override
    public void serialize(Json value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        var text = value.asString();
        log.info("The raw json value from PostgresSQL JSON type:" + text);
        JsonFactory factory = new JsonFactory();
        JsonParser parser  = factory.createParser(text);
        var node = gen.getCodec().readTree(parser);
        serializers.defaultSerializeValue(node, gen);
    }

}
```
The above `PgJsonObjectSerializer` reads the raw json as string which is escaped, and use Jackson to parse it and regenerate to json literal form in the response by Jackson.

```java
@Slf4j
public class PgJsonObjectDeserializer extends JsonDeserializer<Json> {

    @Override
    public Json deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        var value = ctxt.readTree(p);
        log.info("read json value :", value);
        return Json.of(value.toString());
    }
}
```

The `PgJsonObjectDeserializer` converts incoming json string to `Json` object.

Then apply then on the fields of the entity.

```java
public class Post {

    // other fields
    @JsonSerialize(using = PgJsonObjectSerializer.class)
    @JsonDeserialize(using = PgJsonObjectDeserializer.class)
    private Json metadata;
}    
```

Or register them in Jackson `ObjectMapper` configuration.

```java
var builder = Jackson2ObjectMapperBuilder.json();
//...
// register globally or on class fields
builder.serializers(new PgJsonObjectSerializer())
    .deserializers(new PgJsonObjectDeserializer())
```

If it is a Spring Boot application, just need to declare a `@JsonComponent` component, Spring Boot will register it automatically.

```java
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
```

## Enum type

Postgres database support custom types, you can create your enum type and limit the inserting values in a set of predefined  items.

```sql
DO $$ BEGIN
    CREATE TYPE post_status AS ENUM( 'DRAFT', 'PENDING_MODERATION', 'PUBLISHED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;
```

The scripts will try to create a custom type `post_status` and only its value is  only  allowed to set in one of `DRAFT`, `PENDING_MODERATION`,  `PUBLISHED`. 

> The `ConnectionFactoryInitializer` read the scripts by termination of  ";" which caused exceptions when the application starts up. I put these scripts into the init.sql of  Postgres docker service defined in `docker-compose.yml`.

Define a `post_status` enum data type in the table script.

```sql
CREATE TABLE IF NOT EXISTS posts (
    // other fields
    status post_status default 'DRAFT',
    PRIMARY KEY (id)
 );
```

Create a Java enum class and define a `stauts` field in the `Post` entity.

```java
public class Post {
    //...
    @Column("status")
    private Status status;
}

enum Status {
    DRAFT, PENDING_MODERATION, PUBLISHED;
}
```

To encode and decode the enum data type automatically, register a Postgres specific `EnumCodec` in the connection configration which is shipped with Postgres R2dc drivers.

```java
PostgresqlConnectionConfiguration.builder()
    ...
    .codecRegistrar(EnumCodec.builder().withEnum("post_status", Post.Status.class).build())
    .build()
```

Then you can use `DatabaseClient` to read and write data of Java enum type like this.

```java
// read from rowset directly.
row.get("status", Post.Status.class)
    
// bind java Enum type to parameter placeholder directly
.bind("status", p.getStatus())       
```

When using Spring Data R2dbc, it registers a collection of converters in its `MappingContext`, and it will detect the data type and Java type and use these converters when reading and writing data. 

> Ideally when it fails, it will return to use the built-in solution from the baked drivers, but I got exceptions when reading data from database. Follow the official Spring Data R2dbc reference document, you can create your custom converters to resolve this issue.

Create s `@ReadingConverter` to convert the data type to Java type.

```java
@ReadingConverter
public class PostReadingConverter implements Converter<Row, Post> {

    @Override
    public Post convert(Row row) {
        return Post.builder()
                .id(row.get("id", UUID.class))
                .title(row.get("title", String.class))
                .content(row.get("content", String.class))
                .status(row.get("status", Post.Status.class))
                .metadata(row.get("metadata", Json.class))
                .createdAt(row.get("created_at", LocalDateTime.class))
                .updatedAt(row.get("updated_at", LocalDateTime.class))
                .version(row.get("version", Long.class))
                .build();
    }
}
```

Creating a `@WritingConverter` to handle the conversion of Java Enum type to enum data type. Spring Data R2dbc has a `EnumWriteSupport` to simplify the work.

```java
@WritingConverter
public class PostStatusWritingConverter extends EnumWriteSupport<Post.Status> {
}
```

Register them in the R2dbc configuration.

```java
public class DatabaseConfig extends AbstractR2dbcConfiguration {

	//...

    @Override
    protected List<Object> getCustomConverters() {
        return List.of(new PostReadingConverter(), new PostStatusWritingConverter());
    }
}
```

 Spring Data R2dbc can convert Java Enum type to a textual data type and reverse automatically. You can just need to declare the *enum* column as a string type, eg. 

```sql
status VARCHAR(255) default 'DRAFT',
```

No need custom converters in this case.

The disadvantage of this usage is that it will loss the constraints of the database itself, you are responsible for the value inserted in the tables.

## Notifier/Listener

Postgres provides simple notification mechanism, you can use it as  a simple message broker.

Create a `Nofitier` to send a message.

```java
@Component
@Slf4j
class Notifier {
    @Autowired
    @Qualifier("pgConnectionFactory")
    ConnectionFactory pgConnectionFactory;

    PostgresqlConnection sender;

    @PostConstruct
    public void initialize() throws InterruptedException {
        sender = Mono.from(pgConnectionFactory.create())
                .cast(PostgresqlConnection.class)
                .block();
    }

    public Mono<Void> send() {
        return sender.createStatement("NOTIFY mymessage, 'Hello world at " + LocalDateTime.now() + "'")
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
```

And listen is a  `Listener` bean.

```java
@Component
@Slf4j
class Listener {
    @Autowired
    @Qualifier("pgConnectionFactory")
    ConnectionFactory pgConnectionFactory;

    PostgresqlConnection receiver;

    @PostConstruct
    public void initialize() throws InterruptedException {
        receiver = Mono.from(pgConnectionFactory.create())
                .cast(PostgresqlConnection.class)
                .block();

        receiver.createStatement("LISTEN mymessage")
                .execute()
                .flatMap(PostgresqlResult::getRowsUpdated)
                .log("listen::")
                .subscribe();

        receiver.getNotifications()
                .delayElements(Duration.ofSeconds(1))
                .log()
                .subscribe(
                        data -> log.info("notifications: {}", data)
                );
    }

    @PreDestroy
    public void destroy() {
        receiver.close().subscribe();
    }

}
```

Add a  routing to send a message, check the logging of listener side.

```java
.GET("/hello", request -> notifier
     .send()
     .flatMap((v) -> noContent().build())
    )
```

