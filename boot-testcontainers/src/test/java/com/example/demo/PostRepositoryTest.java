package com.example.demo;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest()
@Slf4j
@Import(value = {TestcontainersConfiguration.class})
public class PostRepositoryTest {

    @Autowired
    R2dbcEntityTemplate template;

    @Autowired
    PostRepository posts;

    @Test
    public void testInsertAndQuery() {
        var data = Post.of("test title", "content of test");
        this.template.insert(data)
                .flatMap(p ->
                        this.posts.findById(p.id())
                )
                .log()
                .as(StepVerifier::create)
                .consumeNextWith(p -> {
                            log.info("saved post: {}", p);
                            assertThat(p.title()).isEqualTo("test title");
                        }
                )
                .verifyComplete();

    }
}
