# Spring R2dbc Example

I have created several R2dbc examples in the [spring-reactive-sample](https://github.com/hantsy/spring-reactive-sample/) repository since it was born. But Spring Data R2dbc evolved very quickly, a lot of changes are introduced in the upcoming Spring Data R2dbc 1.2. This repository will contain the latest work in the Spring Framework 5.3, Spring Data R2dbc 1.2 and Spring Boot 2.4.



## Notes

* [Introduction to R2dbc](./docs/intro.md)
* [Working with Relational Database using R2dbc DatabaseClient](./docs/database-client.md)
* [*Update*: Accessing RDBMS with Spring Data R2dbc](./docs/data-r2dbc.md)
* [Data Auditing with Spring Data R2dbc](./docs/auditing.md)
* [Dealing with Postgres specific Json/Enum type and NOTIFY/LISTEN with R2dbc](./docs/pg.md)

  

## Sample Codes

* [boot](https://github.com/hantsy/spring-r2dbc-sample/tree/master/boot) Spring Boot example
* [auditing](https://github.com/hantsy/spring-r2dbc-sample/tree/master/auditing) Spring Data R2dbc Auditing example
* [database-client](https://github.com/hantsy/spring-r2dbc-sample/tree/master/database-client) `DatabaseClient` example
* [data-r2dbc-entitytemplates](https://github.com/hantsy/spring-r2dbc-sample/tree/master/data-r2dbc-entitytemplate) Spring Data R2dbc specific `R2dbcEntityTemplate` example
* [data-r2dbc-repositories](https://github.com/hantsy/spring-r2dbc-sample/tree/master/data-r2dbc-repositories) Spring Data R2dbc generic `R2dbcRepository` interface example
* [connection-factories](https://github.com/hantsy/spring-r2dbc-sample/tree/master/connection-factories) R2dbc driver's `ConnectionFactory` example
* [entitycallbacks](https://github.com/hantsy/spring-r2dbc-sample/tree/master/entitycallbacks) Spring Data R2dbc `BeforeConvertEntityCallback`, `AfterConvertEntityCallback`, etc.

## Reference

* [pgjdbc/r2dbc-postgresql](https://github.com/pgjdbc/r2dbc-postgresql)
* [R2dbc spec ](https://r2dbc.io/spec/0.8.2.RELEASE/spec/html/)
* [A Practical Guide to MySQL JSON Data Type By Example](https://www.mysqltutorial.org/mysql-json/)
* [Convert between Java enums and PostgreSQL enums](https://www.gotoquiz.com/web-coding/programming/java-programming/convert-between-java-enums-and-postgresql-enums/)
* [Java & Postgres enums - How do I make them work together for update?](https://stackoverflow.com/questions/40356750/java-postgres-enums-how-do-i-make-them-work-together-for-update)
* [How to delete an enum type value in postgres?](https://stackoverflow.com/questions/25811017/how-to-delete-an-enum-type-value-in-postgres)
* [Create a type if not exist Postgresql](https://stackoverflow.com/questions/56647514/create-a-type-if-not-exist-postgresql)
