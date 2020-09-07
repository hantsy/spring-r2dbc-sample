package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * @author hantsy
 */
@Slf4j
public class PostRepositoryTest {

    @Test
    public void testGetAllPostsDeepStubs() {
        // RETURNS_DEEP_STUBS is used for mock sql, filter, all together in a stubbing statement.
        var template = mock(R2dbcEntityTemplate.class, RETURNS_DEEP_STUBS);
        var posts = new PostRepository(template);
        log.info("databaseClient: {}", template);
        given(template.select(Post.class).all())
                .willReturn(Flux.just(Post.builder().title("test").content("content").build()));

        var result = posts.findAll();

        assertThat(result).isNotNull();
        result.as(StepVerifier::create)
                .consumeNextWith(p -> assertThat(p.getTitle()).isEqualTo("test"))
                .verifyComplete();
    }

}
