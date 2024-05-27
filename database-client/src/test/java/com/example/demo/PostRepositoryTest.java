package com.example.demo;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author hantsy
 */
@Slf4j
@SpringJUnitConfig(classes = {DatabaseConfig.class, PostRepositoryTest.TestConfig.class})
public class PostRepositoryTest {

    @Autowired
    PostRepository posts;

    @SneakyThrows
    @BeforeEach
    public void setup() {
        var latch = new CountDownLatch(1);
        this.posts.deleteAll()
                .doOnTerminate(latch::countDown)
                .subscribe(
                        data -> log.info("clean database: {} deleted.", data)
                );
        latch.await(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testSaveAll() {

        var data = Post.builder().title("test").content("content")
                .status(Post.Status.PENDING_MODERATION)
                .build();
        var data1 = Post.builder().title("test1").content("content1").build();

        var result = posts.saveAll(List.of(data, data1)).log("[Generated result]")
                .doOnNext(id -> log.info("generated id: {}", id));

        assertThat(result).isNotNull();
        result.as(StepVerifier::create)
                .expectNextCount(2)
                .verifyComplete();

        StepVerifier.create(posts.countByStatus())
                .consumeNextWith(r -> {
                    log.info("data: {}", r);
                    assertThat(r.get("status")).isEqualTo(Post.Status.DRAFT);
                })
                .consumeNextWith(r -> {
                    log.info("data: {}", r);
                    assertThat(r.get("cnt")).isEqualTo(1L);
                    assertThat(r.get("status")).isEqualTo(Post.Status.PENDING_MODERATION);
                })
                .verifyComplete();
    }

    //see: https://stackoverflow.com/questions/64374730/java-r2dbc-client-execute-sql-and-use-returned-id-for-next-execute/64409363#64409363
    @Test
    public void testInsertAndQuery() {
        var data = Post.builder().title("test").content("content")
                .status(Post.Status.PENDING_MODERATION)
                .build();
        this.posts.save(data)
                .flatMap(id -> this.posts.findById(id))
                .as(StepVerifier::create)
                .consumeNextWith(r -> {
                    log.info("result data: {}", r);
                    assertThat(r.getStatus()).isEqualTo(Post.Status.PENDING_MODERATION);
                })
                .verifyComplete();
    }

    @Test
    public void testInsertAndDelete() {
        var data = Post.builder().title("test").content("content")
                .status(Post.Status.PENDING_MODERATION)
                .build();
        this.posts.save(data)
                .flatMap(id -> this.posts.deleteAllById(List.of(id)))
                .as(StepVerifier::create)
                .consumeNextWith(r -> {
                    log.info("deleted result: {}", r);
                    assertThat(r).isGreaterThan(0);
                })
                .verifyComplete();
    }

    @ComponentScan
    static class TestConfig {
    }

}
