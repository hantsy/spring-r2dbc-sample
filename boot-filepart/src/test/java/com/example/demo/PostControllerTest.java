package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebFluxTest(controllers = PostController.class)
public class PostControllerTest {

    @MockBean
    private PostRepository postRepository;

    @Autowired
    WebTestClient client;

    @Test
    public void createPost() {
        var post = Post.builder()
                .title("test")
                .content("test").build();
        post.setId(UUID.randomUUID());

        when(postRepository.save(any()))
                .thenReturn(Mono.just(post));

        this.client.post()
                .uri("/posts")
                .bodyValue(new CreatePostCommand("test", "test"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION);

        verify(postRepository, times(1)).save(any());
        verifyNoMoreInteractions(postRepository);
    }
}
