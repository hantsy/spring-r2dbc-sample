package com.example.demo;

import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

@RequiredArgsConstructor
@Component
@Slf4j
public class PostRepository {

    public static final BiFunction<Row, RowMetadata, Post> MAPPING_FUNCTION = (row, rowMetaData) -> Post.builder()
            .id(row.get("id", UUID.class))
            .title(row.get("title", String.class))
            .content(row.get("content", String.class))
            .status(row.get("status", Post.Status.class))
            .metadata(row.get("metadata", Json.class))
            .createdAt(row.get("created_at", LocalDateTime.class))
            .build();

    private final DatabaseClient databaseClient;

    public Flux<Post> findByTitleContains(String name) {
        return this.databaseClient
                .sql("SELECT * FROM posts WHERE title LIKE :title")
                .bind("title", "%" + name + "%")
                .map(MAPPING_FUNCTION)
                .all();
    }

    public Flux<Post> findAll() {
        return this.databaseClient
                .sql("SELECT * FROM posts")
                .filter((statement, executeFunction) -> statement.fetchSize(10).execute())
                .map(MAPPING_FUNCTION)
                .all();
    }

    // see:
    // https://stackoverflow.com/questions/64267699/spring-data-r2dbc-and-group-by
    public Flux<Map<Object, Object>> countByStatus() {
        return this.databaseClient
                .sql("SELECT count(*) as cnt, status FROM posts group by status")
                .map((row, rowMetadata) -> {
                    Long cnt = row.get("cnt", Long.class);
                    Post.Status s = row.get("status", Post.Status.class);

                    return Map.<Object, Object>of("cnt", cnt, "status", s);
                })
                .all();
    }

    public Mono<Post> findById(UUID id) {
        return this.databaseClient
                .sql("SELECT * FROM posts WHERE id=:id")
                .bind("id", id)
                .map(MAPPING_FUNCTION)
                .one();
    }

    public Mono<UUID> save(Post p) {
        return this.databaseClient.sql(
                "INSERT INTO  posts (title, content, metadata, status) VALUES (:title, :content, :metadata, :status)")
                .filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
                .bind("title", p.getTitle())
                .bind("content", p.getContent())
                .bind("metadata", p.getMetadata())
                .bind("status", p.getStatus())
                .fetch()
                .first()
                .map(r -> (UUID) r.get("id"));
    }

    // see: https://github.com/spring-projects/spring-data-r2dbc/issues/259
    // and
    // https://stackoverflow.com/questions/62514094/how-to-execute-multiple-inserts-in-batch-in-r2dbc
    public Flux<UUID> saveAll(List<Post> data) {
        Assert.notEmpty(data, "saving data can be empty");
        return this.databaseClient.inConnectionMany(connection -> {

            var statement = connection
                    .createStatement("INSERT INTO  posts (title, content, status) VALUES ($1, $2, $3)")
                    .returnGeneratedValues("id");

            for (int i = 0; i < data.size() - 1; i++) {
                Post p = data.get(i);
                statement.bind(0, p.getTitle())
                        .bind(1, p.getContent())
                        .bind(2, p.getStatus())
                        .add();
            }

            // for the last item, do not call `add`
            var lastItem = data.get(data.size() - 1);
            statement.bind(0, lastItem.getTitle())
                    .bind(1, lastItem.getContent())
                    .bind(2, lastItem.getStatus());

            return Flux.from(statement.execute())
                    .flatMap(result -> result.map((row, rowMetadata) -> row.get("id", UUID.class)));
        });
    }

    public Mono<Long> update(Post p) {
        return this.databaseClient
                .sql("UPDATE posts set title=:title, content=:content, metadata=:metadata, status=:status WHERE id=:id")
                .bind("title", p.getTitle())
                .bind("content", p.getContent())
                .bind("metadata", p.getMetadata())
                .bind("status", p.getStatus())
                .bind("id", p.getId())
                .fetch()
                .rowsUpdated();
    }

    public Mono<Long> deleteById(UUID id) {
        return this.databaseClient.sql("DELETE FROM posts WHERE id=:id")
                .bind("id", id)
                .fetch()
                .rowsUpdated();
    }

    public Mono<Long> deleteAll() {
        return this.databaseClient.sql("DELETE FROM posts")
                .fetch()
                .rowsUpdated();
    }
}
