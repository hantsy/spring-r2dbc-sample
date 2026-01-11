package com.example.demo;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataR2dbcTest
@Import(TestcontainersConfiguration.class)
@Slf4j
public class TodoRepositoryTest {

    @Autowired
    TodoRepository todos;

    @SneakyThrows
    @BeforeEach
    public void setup() {
        var latch = new CountDownLatch(1);
        this.todos.deleteAll()
            .doOnTerminate(latch::countDown)
            .subscribe(data -> log.debug("post data is removed"), err -> log.error("error:" + err));
        latch.await(500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testTodosRepositoryExisted() {
        assertNotNull(todos);
    }

    @Test
    public void testInsertAndQuery() {
        var data = new Todo(null, "test title");

        todos.save(data)
            .thenMany(this.todos.findAll())
            .as(StepVerifier::create)
            .consumeNextWith(
                p -> {
                    log.info("saved post: {}", p);
                    assertThat(p.title()).isEqualTo("test title");
                }
            )
            .verifyComplete();
    }
}
