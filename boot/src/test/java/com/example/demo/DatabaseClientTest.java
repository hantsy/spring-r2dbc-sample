package com.example.demo;


import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataR2dbcTest()
@Slf4j
@Import(value = {TestcontainersConfiguration.class, DataConfig.class})
public class DatabaseClientTest {

    static final BiFunction<Row, RowMetadata, Post> MAPPING_FUNCTION = (row, _) -> new Post(
            row.get("id", UUID.class),
            row.get("title", String.class),
            row.get("content", String.class),
            Post.Status.valueOf(row.get("status", String.class)),
            row.get("created_at", LocalDateTime.class),
            row.get("created_by", String.class),
            row.get("updated_at", LocalDateTime.class),
            row.get("updated_by", String.class),
            row.get("version", Long.class)
    );

    @Autowired
    DatabaseClient databaseClient;

    @Test
    public void testInsertAndQuery_template() {
        var data = Post.of("test title", "content of test");
        var insertSql = """
                INSERT INTO posts(title, content)
                VALUES (:title, :content)
                """;
        var selectOneSql = """
                SELECT * FROM posts WHERE id=:id
                """;
        this.databaseClient.sql(insertSql)
                .filter((statement, _) -> statement.returnGeneratedValues("id").execute())
                .bindValues(Map.of("title", "test title", "content", "test content"))
                .fetch()
                .one()
                .map(res -> (UUID) (res.get("id")))
                .flatMap(id -> this.databaseClient.sql(selectOneSql)
                        .bind("id", id)
                        .map(MAPPING_FUNCTION)
                        .one()
                )
                .log()
                .as(StepVerifier::create)
                .consumeNextWith(p -> {
                            log.info("saved post: {}", p);
                            assertThat(p.title()).isEqualTo("test title");

                            // the auditing does not apply the DatabaseClient
                            assertNull(p.createdAt());
                            assertNull(p.updatedAt());
                            assertThat(p.createdBy()).isNull();
                            assertThat(p.updatedBy()).isNull();
                        }
                )
                .verifyComplete();
    }
}
