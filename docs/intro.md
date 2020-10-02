

# Introduction to R2dbc 



In contrast to the blocking nature of Jdbc, R2dbc allows you to access with relational databases using none-blocking APIs, R2dbc embraces the [Reactive Streams](https://www.reactive-streams.org/) spec, and provides a community-driven open specification which includes a collection of  Service Provider Interface(SPI) to the database vendors to implement their own drivers.

To connect to databases, you should add corresponding drivers into your project dependencies. 

## R2dbc drivers

Currently there are a few drivers ready for production, check the [R2dbc drivers](https://r2dbc.io/drivers/) page for the complete list.

H2 database is frequently used in development environment, add the following dependency when using either embedded or file-based H2 database.

```xml
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-h2</artifactId>
</dependency>
```

To use R2dbc with MySQL database, add the following dependency instead.

```xml
<dependency>
    <groupId>dev.miku</groupId>
    <artifactId>r2dbc-mysql</artifactId>
</dependency>
```

R2dbc also supports  [MariaDB](https://github.com/mariadb-corporation/mariadb-connector-r2dbc) -  the MySQL fork .

For Postgres database, use Postgres R2dbc driver.

```xml
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
</dependency>
```

R2dbc has good support of  Microsoft SQL Server.

```xml
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-mssql</artifactId>
</dependency>
```

If you have installed desired databases, and make sure it is running. Now you can connect it via R2dbc's `ConnectionFactory`.

## Connecting to Databases

Internally R2dbc spec provides a `ConnectionFactories` utility class to create a `ConnectionFactory` from connection URL or `ConnectionFactoryOptions`. 

The following is an example of obtaining a H2 specific `ConnectionFactory` from URL, it will connect to an embedded H2 database.

```java
ConnectionFactories.get("r2dbc:h2:mem:///test?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
```

Let's look at another example of Postgres connection URL.

```java
ConnectionFactories.get("r2dbc:postgres://user:password@localhost/test");
```

The details of the URL form is described in the [R2dbc spec doc](https://r2dbc.io/spec/0.8.2.RELEASE/spec/html/#overview.connection.url).

The following is an example of getting a `ConnectionFactory` from `ConnectionFactoryOptions`. 

```java
var options = ConnectionFactoryOptions.builder()
    .option(ConnectionFactoryOptions.HOST, "localhost")
    .option(ConnectionFactoryOptions.DATABASE, "test")
    .option(ConnectionFactoryOptions.USER, "user")
    .option(ConnectionFactoryOptions.PASSWORD, "password")
    .option(ConnectionFactoryOptions.DRIVER, "postgresql")
    //.option(PostgresqlConnectionFactoryProvider.OPTIONS, Map.of("lock_timeout", "30s"))
    .build();
return ConnectionFactories.get(options);
```

Most of the R2dbc drivers provide its private utility classes to create a `ConnectionFactory`.

For example, you can create an embedded or file-based H2 database using `H2ConnectionFactory` like this.

```java
H2ConnectionFactory.inMemory("test");

//file
new H2ConnectionFactory(
    H2ConnectionConfiguration.builder()
    //.inMemory("testdb")
    .file("./testdb")
    .username("sa")
    .password("password")
    .build()
)
```

 Similarly Postgres drivers also provides a `PostgresConnectionFactory` to create a `ConnectionFactory` instance.

```java
new PostgresqlConnectionFactory(
    PostgresqlConnectionConfiguration.builder()
    .host("localhost")
    .database("test")
    .username("user")
    .password("password")
    //.codecRegistrar(EnumCodec.builder().withEnum("post_status", Post.Status.class).build())
    .build()
);
```

The driver specific utility class is more easy to setup database specific features, eg. enable the `Enum` codec in Postgres.

Once you get a `ConnectionFactory`,  calling the `create` method to create a `Publisher<Connection>`, which is similar to the Jdbc's Connection, but is based on Reactive Streams APIs.

## Executing SQL queries

Utilizing with the `Connection.createStatement`, you can execute SQL queries like insert, update or delete etc.  The following example is creating a table and then inserting some data into  the table on a H2 database. 

```java
String createSql = """
    CREATE TABLE IF NOT EXISTS persons (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    age INTEGER
	)
    """;

String insertSql = """
    INSERT INTO persons(first_name, last_name, age)
    VALUES
    ('Hello', 'Kitty', 20),
    ('Hantsy', 'Bai', 40)
    """;
        
Mono.from(conn)   
    .flatMap(
        c -> Mono.from(c.createStatement(createSql).execute())
	)
    .log()
    .doOnNext(data -> log.info("created: {}", data))
    .then()
    .thenMany(
        Mono.from(conn)
        	.flatMapMany(
            	c -> c.createStatement(insertSql)
                    .returnGeneratedValues("id")
                    .execute()
            )
    )
    .flatMap(data -> Flux.from(data.map((row, rowMetadata) -> row.get("id"))))
    .doOnNext(id -> log.info("generated id: {}", id))
```

In the above codes, we used [Project Reactor](https://projectreactor.io/) to wrap the connection and simplify the Reactive Streams pipeline operations. The `returnGeneratedValues("id")` method will fetch the generated id when inserting a row into a table.

Let's have  a look at another *select* statement.

```
var selectSql = """
    SELECT * FROM persons;
    """;
    
Mono.from(conn)
    .flatMapMany(
        c -> Flux.from(c.createStatement(selectSql).execute())
    )
    .log()
    .flatMap(result -> result
    	.map((row, rowMetadata) -> {
    		rowMetadata.getColumnMetadatas()
    			.forEach(
   					columnMetadata -> log.info("column name:{}, type: {}", columnMetadata.getName(), 		columnMetadata.getJavaType())
    	);
    
        var id = row.get("id", Integer.class);
        var firstName = row.get("first_name", String.class);
        var lastName = row.get("last_name", String.class);
        var age = row.get("age", Integer.class);

    	return Person.builder().firstName(firstName)
            .lastName(lastName)
            .age(age)
            .id(id)
            .build();
    }))
    .doOnNext(data -> log.info(": {}", data))
```

After it is executed, to extract the row data from the query result, you can call the  `map` method to get the details of row data and row metadata.

You can also bind parameters to placeholders in SQL query strings. 

```java
var selectSql = """
    SELECT * FROM persons WHERE first_name=$1
    """;
    
Mono.from(conn)
    .flatMapMany(
        c -> Flux.from(c.createStatement(selectSql)
                       .bind("$1", "Hantsy")
                       .execute())
    )
```

> H2 accepts `$`  as prefix in the parameter placeholders. But it is database specific, for MSSQL database, the prefix should be a `@`.

When executing a *update* query, you can get the number of updated rows.

```java
var updateSql = """
    UPDATE persons SET first_name=$1 WHERE first_name=$2
    """;

Mono.from(conn)
        .flatMapMany(
            c -> Flux.from(c.createStatement(updateSql)
                           .bind("$1", "test1")
                           .bind("$2", "Hantsy")
                           .execute())
        .flatMap(result -> result.getRowsUpdated())
        .doOnNext(data -> log.info(": {}", data))
    )
    .log()
```

Similarly executing a *delete* query is also easy to get the number of affected rows.

## R2dbc Clients

There are some client libraries which wraps R2dbc's `Connection`, `Statement` etc, and hide the complexity of the raw R2dbc APIs.

In Spring, there is a new refactored `DatabaseClient` brought in the upcoming Spring 5.3.  Spring Data R2dbc provides a light ORM mapping features based on `DatabaseClient`.

We will explore the `DatabaseClient`  in the next post.

You can get the [sample codes](https://github.com/hantsy/spring-r2dbc-sample/) from my github.