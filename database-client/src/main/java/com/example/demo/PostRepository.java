package com.example.demo;

import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
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

    public Mono<Post> findById(UUID id) {
        return this.databaseClient
                .sql("SELECT * FROM posts WHERE id=:id")
                .bind("id", id)
                .map(MAPPING_FUNCTION)
                .one();
    }

    public Mono<UUID> save(Post p) {
        return this.databaseClient.sql("INSERT INTO  posts (title, content, metadata) VALUES (:title, :content, :metadata)")
                .filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
                .bind("title", p.getTitle())
                .bind("content", p.getContent())
                .bind("metadata", p.getMetadata())
                .fetch()
                .first()
                .map(r -> (UUID) r.get("id"));
    }

    public Flux<UUID> saveAll(List<Post> data) {
        return this.databaseClient.inConnectionMany(connection -> {

            var statement = connection.createStatement("INSERT INTO  posts (title, content) VALUES ($1, $2)")
                    .returnGeneratedValues("id");

            for (var p : data) {
                statement.bind(0, p.getTitle()).bind(1, p.getContent()).add();
            }

            return Flux.from(statement.execute()).flatMap(result -> result.map((row, rowMetadata) -> row.get("id", UUID.class)));
        });
    }

    public Mono<Integer> update(Post p) {
        return this.databaseClient.sql("UPDATE posts set title=:title, content=:content, metadata=:metadata, status=:status WHERE id=:id")
                .bind("title", p.getTitle())
                .bind("content", p.getContent())
                .bind("metadata", p.getMetadata())
                .bind("status", p.getStatus())
                .bind("id", p.getId())
                .fetch()
                .rowsUpdated();
    }

    public Mono<Integer> deleteById(UUID id) {
        return this.databaseClient.sql("DELETE FROM posts WHERE id=:id")
                .bind("id", id)
                .fetch()
                .rowsUpdated();
    }
}