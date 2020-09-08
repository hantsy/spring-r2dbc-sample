package com.example.demo;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.test.StepVerifier;

import java.time.Duration;

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
}
