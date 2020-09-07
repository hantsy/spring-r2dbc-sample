package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

/**
 * @author hantsy
 */
@SpringJUnitConfig(classes = {Jackson2ObjectMapperConfig.class, WebConfig.class, PostRouterFunctionTest.TestConfig.class})
@ExtendWith(MockitoExtension.class)
public class PostRouterFunctionTest {

    @Autowired
    RouterFunction<ServerResponse> routerFunction;

    WebTestClient client;

    @BeforeEach
    public void setup() {
        this.client = WebTestClient
                .bindToRouterFunction(routerFunction)
                .configureClient()
                .build();
    }

    @Test
    public void getAllPostsWillBeOk() throws Exception {
        this.client
                .get().uri("/posts").accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                    .jsonPath("$.length()").isEqualTo(1)
                    .jsonPath("$[0].title").isEqualTo("test");
    }


    @Configuration
    static class TestConfig {

        @Bean
        PostHandler postHandler() {
            var handler = mock(PostHandler.class);
            given(handler.all(any(ServerRequest.class)))
                    .willReturn(ok().body(Flux.just(Post.builder().title("test").content("content").build()), Post.class));
            return handler;
        }
    }

}
