package com.example.demo;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest()
@Slf4j
@Import(value = {TestcontainersConfiguration.class, R2dbcProxyConfig.class})
public class PostRepositoryTest {

    @Autowired
    PostRepository posts;

    @Test
    public void testInsertAndQuery() {
        var data = Post.of("test title", "content of test");
        this.posts.save(data)
                .flatMap(p ->
                        this.posts.findById(p.id())
                )
                .log()
                .as(StepVerifier::create)
                .consumeNextWith(p -> {
                            log.info("saved post: {}", p);
                            assertThat(p.title()).isEqualTo("test title");
                            // Check the logging to see the proxy calling
                        }
                )
                .verifyComplete();

    }
}
