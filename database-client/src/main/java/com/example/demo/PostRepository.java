package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
public class PostRepository {

    private final DatabaseClient databaseClient;

    Flux<Post> findByTitleContains(String name) {
        return this.databaseClient
                .sql("select * from posts where title like :title")
                .bind("title", "%" + name + "%")
                .map(row -> Post.builder()
                        .id(row.get("id", Integer.class))
                        .title(row.get("title", String.class))
                        .content(row.get("content", String.class))
                        .build()
                )
                .all();

    }

    public Flux<Post> findAll() {
        return this.databaseClient
                .sql("select * from posts")
                .map(row -> Post.builder()
                        .id(row.get("id", Integer.class))
                        .title(row.get("title", String.class))
                        .content(row.get("content", String.class))
                        .build()
                )
                .all();
    }

    public Mono<Post> findById(Integer id) {
        return this.databaseClient
                .sql("select * from posts where id=:id")
                .bind("id", id)
                .map(row -> Post.builder()
                        .id(row.get("id", Integer.class))
                        .title(row.get("title", String.class))
                        .content(row.get("content", String.class))
                        .build()
                )
                .one();
    }

    public Mono<Integer> save(Post p) {
        return this.databaseClient.sql("INSERT INTO  posts (title, content) VALUES (:title, :content)")
                .filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
                .bind("title", p.getTitle())
                .bind("content", p.getContent())
                .fetch()
                .first()
                .map(stringObjectMap -> (Integer) stringObjectMap.get("id"));
    }

    public Mono<Integer> update(Post p) {
        return this.databaseClient.sql("UPDATE posts set title=:title, content=:content where id=:id")
                .bind("title", p.getTitle())
                .bind("content", p.getContent())
                .bind("id", p.getId())
                .fetch()
                .rowsUpdated();
    }

    public Mono<Integer> deleteById(Integer id) {
        return this.databaseClient.sql("DELETE FROM posts where id=:id")
                .bind("id", id)
                .fetch()
                .rowsUpdated();
    }
}