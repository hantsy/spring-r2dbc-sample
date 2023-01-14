# Spring R2dbc Example

>This repository contains the latest changes in the Spring R2dbc(part of Spring Framework 5.3), Spring Data R2dbc 1.2 and Spring Boot 2.4.

I have created several R2dbc examples in the [spring-reactive-sample](https://github.com/hantsy/spring-reactive-sample/) repository since it was born. But Spring Data R2dbc evolved very quickly, thre are plenty of breaking changes introduced since Spring 5.2 and Spring Data R2dbc 1.2.

Compare to the Spring Data R2dbc 1.1, some breaking changes are notable.
* The old DatabaseClient in Spring Data R2dbc 1.1 was split into two parts, a simple new `DatabaseClient` is part of Spring framework, as an alternative of Jdbc.
* Another part of the old DatabaseClient is reorganized into a new class `R2dbcEntityTemplate` which acts as the role of `JdbcTemplate`.

## Notes

* [Introduction to R2dbc](./docs/intro.md)
* [Working with Relational Database using R2dbc DatabaseClient](./docs/database-client.md)
* [*Update*: Accessing RDBMS with Spring Data R2dbc](./docs/data-r2dbc.md)
* [Data Auditing with Spring Data R2dbc](./docs/auditing.md)
* [Dealing with Postgres specific Json/Enum type and NOTIFY/LISTEN with R2dbc](./docs/pg.md)
* [Building Chat Application with R2dbc and Postgres](./docs/chat.md)
  

## Sample Codes
| Example | Description |
|---|---|
| [connection-factories](https://github.com/hantsy/spring-r2dbc-sample/tree/master/connection-factories)  | R2dbc driver's `ConnectionFactory` examples for MySQL, H2, PostgreSQL, MSSQL, Oracle, etc|
| [database-client](https://github.com/hantsy/spring-r2dbc-sample/tree/master/database-client) |  Spring R2dbc `DatabaseClient` example |
| [data-r2dbc-entitytemplates](https://github.com/hantsy/spring-r2dbc-sample/tree/master/data-r2dbc-entitytemplate) |  Spring Data R2dbc  `R2dbcEntityTemplate` example |
| [data-r2dbc-repositories](https://github.com/hantsy/spring-r2dbc-sample/tree/master/data-r2dbc-repositories)  | Spring Data R2dbc `R2dbcRepository` interface example |
| [entitycallbacks](https://github.com/hantsy/spring-r2dbc-sample/tree/master/entitycallbacks)  | Spring Data R2dbc `BeforeConvertEntityCallback`, `AfterConvertEntityCallback`, etc. |
| [testcontainers](https://github.com/hantsy/spring-r2dbc-sample/tree/master/testcontainers) | Spring Data R2dbc `@DataR2dbcTest` with Testcontainers. |
| [boot](https://github.com/hantsy/spring-r2dbc-sample/tree/master/boot) | Spring Boot example (with Postgres specific `Enum`, `Json`, `NOTIFY/LISTEN`, etc.)|
| [boot-filepart](https://github.com/hantsy/spring-r2dbc-sample/tree/master/boot-filepart) | Spring Boot FilePart example, Postgres `bytea` type mapping to `byte[]`, `ByteBuffer`, R2dbc `Blob`|
| [r2dbc-migrate](https://github.com/hantsy/spring-r2dbc-sample/tree/master/r2dbc-migrate) | [R2dbc Migrate](https://github.com/nkonev/r2dbc-migrate) example|
| [auditing](https://github.com/hantsy/spring-r2dbc-sample/tree/master/auditing)  | Spring Data R2dbc Auditing example |
| [kotlin-co](https://github.com/hantsy/spring-r2dbc-sample/tree/master/kotlin-co)  | Kotlin Coroutines example |
| [jooq](https://github.com/hantsy/spring-r2dbc-sample/tree/master/jooq)  | R2dbc and JOOQ example |
| [jooq-kotlin-co-gradle](https://github.com/hantsy/spring-r2dbc-sample/tree/master/jooq-kotlin-co-gradle)  | R2dbc/JOOQ/Kotlin Coroutines and Gradle generator config example |
| [bookstore](https://github.com/hantsy/spring-r2dbc-sample/tree/master/bookstore)  | **(WIP)** An example to track the associations support of Spring Data  R2dbc |

## Reference

* [pgjdbc/r2dbc-postgresql](https://github.com/pgjdbc/r2dbc-postgresql)
* [R2dbc spec ](https://r2dbc.io/spec/0.8.2.RELEASE/spec/html/)
* [A Practical Guide to MySQL JSON Data Type By Example](https://www.mysqltutorial.org/mysql-json/)
* [Convert between Java enums and PostgreSQL enums](https://www.gotoquiz.com/web-coding/programming/java-programming/convert-between-java-enums-and-postgresql-enums/)
* [Java & Postgres enums - How do I make them work together for update?](https://stackoverflow.com/questions/40356750/java-postgres-enums-how-do-i-make-them-work-together-for-update)
* [How to delete an enum type value in postgres?](https://stackoverflow.com/questions/25811017/how-to-delete-an-enum-type-value-in-postgres)
* [Create a type if not exist Postgresql](https://stackoverflow.com/questions/56647514/create-a-type-if-not-exist-postgresql)
* [jOOQ#12218: Reactive transaction with Spring and R2dbc](https://github.com/jOOQ/jOOQ/issues/12218)
