package com.example.demo;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataR2dbcTest()
@Slf4j
public class PostRepositoryTest {

    @Autowired
    R2dbcEntityTemplate template;

    @Autowired
    PostRepository posts;

    @BeforeEach
    public void setup() {
        this.template.delete(Post.class).all().block(Duration.ofSeconds(5));
    }

    @Test
    public void testDatabaseClientExisted() {
        assertNotNull(template);
    }

    @Test
    public void testPostRepositoryExisted() {
        assertNotNull(posts);
    }

    @Test
    public void testInsertAndQuery() {
        this.template.insert(Post.builder().title("test title").content("content of test").build())
                .log()
                .then()
                .thenMany(
                        this.posts.findByTitleContains("test%")
                )
                .log()
                .as(StepVerifier::create)
                .consumeNextWith(p -> {
                    assertEquals("test title", p.getTitle());
                    assertNotNull(p.getCreatedAt());
                    assertNotNull(p.getUpdatedAt());
                })
                .verifyComplete();

    }

    @Test
    public void testInsertAndCount() {
        this.template.insert(Post.builder().title("test title").content("content of test").build())
                .log()
                .then()
                .then(
                        this.posts.countByTitleContaining("test")
                )
                .log()
                .as(StepVerifier::create)
                .consumeNextWith(p -> {
                    assertThat(p).isEqualTo(1);
                })
                .verifyComplete();

    }

    @Test
    public void testInsertAndFindByTitleLike() {
        var data = IntStream.range(1, 101)
                .mapToObj(
                   i-> Post.builder().title("test title#"+i).content("content of test").build()
                )
                .collect(toList());
        this.posts.saveAll(data)
                .log()
                .then()
                .thenMany(
                        this.posts.findByTitleLike("test%", PageRequest.of(0, 10))
                )
                .log()
                .as(StepVerifier::create)
                .expectNextCount(10)
                .verifyComplete();

    }
}
