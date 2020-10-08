package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author hantsy
 */
@Slf4j
@SpringJUnitConfig(classes = {DatabaseConfig.class, PostRepositoryTest.TestConfig.class})
public class PostRepositoryTest {

    @Autowired
    PostRepository posts;

    @Test
    public void testSaveAll() {

        var data = Post.builder().title("test").content("content").build();
        var data1 = Post.builder().title("test1").content("content1").build();

        var result = posts.saveAll(List.of(data, data1)).log("[Generated result]")
                .doOnNext(id->log.info("generated id: {}", id));

        assertThat(result).isNotNull();
        result.as(StepVerifier::create)
                .expectNextCount(2)
                .verifyComplete();
    }

    @ComponentScan
    static class TestConfig {
    }

}
