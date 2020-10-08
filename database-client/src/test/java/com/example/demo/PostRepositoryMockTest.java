package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.StatementFilterFunction;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * @author hantsy
 */
@Slf4j
//@ExtendWith(MockitoExtension.class)
public class PostRepositoryMockTest {

//    @Mock
//    DatabaseClient databaseClient;
//
//    @InjectMocks
//    PostRepository posts;
//
//    @BeforeEach
//    public void setup() {
//
//    }

    @Test
    public void testGetAllPostsDeepStubs() {
        // RETURNS_DEEP_STUBS is used for mock sql, filter, all together in a stubbing statement.
        var databaseClient = mock(DatabaseClient.class, RETURNS_DEEP_STUBS);
        var posts = new PostRepository(databaseClient);
        log.info("databaseClient: {}", databaseClient);
        given(
                databaseClient
                        .sql(anyString())
                        .filter(isA(StatementFilterFunction.class))
                        .map(isA(BiFunction.class))
                        .all())
                .willReturn(Flux.just(Post.builder().title("test").content("content").build()));

        var result = posts.findAll();

        assertThat(result).isNotNull();
        result.as(StepVerifier::create)
                .consumeNextWith(p -> assertThat(p.getTitle()).isEqualTo("test"))
                .verifyComplete();
    }

}
