package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class DemoApplicationIT {

    private WebTestClient client;

    @BeforeEach
    public void setup() {
        this.client = WebTestClient.bindToServer()
            .baseUrl("http://localhost:8080")
            .build();
    }

    @Test
    public void testGetAllPosts() {
        this.client.get().uri("/posts")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(Post.class).hasSize(2);
    }

    @Test
    public void testGetPostByNonExistedID() {
        this.client.get().uri("/posts/{id}", UUID.randomUUID())
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    public void testPostCurdFlow() {
        var id = UUID.randomUUID();
        String title = "Post test " + id;
        String content = "content of " + title;

        var result = client.post().uri("/posts")
            .bodyValue(CreatePostCommand.of(title, content))
            .exchange()
            .expectStatus().isCreated()
            .returnResult(Void.class);

        String savedPostUri = result.getResponseHeaders().getLocation().toString();
        log.debug("saved post location: {}", savedPostUri);
        assertNotNull(savedPostUri);

        client.get().uri(savedPostUri)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.title").isEqualTo(title)
            .jsonPath("$.content").isEqualTo(content);

        String updatedTitle = "updated title";
        String updatedContent = "updated content";
        client.put()
            .uri(savedPostUri)
            .bodyValue(UpdatePostCommand.of(updatedTitle, updatedContent))
            .exchange()
            .expectStatus().isNoContent();

        // verified updated.
        client.get()
            .uri(savedPostUri)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.title").isEqualTo(updatedTitle)
            .jsonPath("$.content").isEqualTo(updatedContent);

        //delete
        client.delete().uri(savedPostUri)
            .exchange()
            .expectStatus().isNoContent();

        //verify it is deleted.
        client.get()
            .uri(savedPostUri)
            .exchange()
            .expectStatus().isNotFound();
    }

}
