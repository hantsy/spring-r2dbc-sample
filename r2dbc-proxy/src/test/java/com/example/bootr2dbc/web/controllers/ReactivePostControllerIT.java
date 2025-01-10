package com.example.bootr2dbc.web.controllers;

import static org.hamcrest.Matchers.hasSize;

import com.example.bootr2dbc.common.AbstractIntegrationTest;
import com.example.bootr2dbc.entities.ReactivePost;
import com.example.bootr2dbc.model.ReactivePostRequest;
import com.example.bootr2dbc.repositories.ReactiveCommentsRepository;
import com.example.bootr2dbc.repositories.ReactivePostRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;

class ReactivePostControllerIT extends AbstractIntegrationTest {

    @Autowired private ReactivePostRepository reactivePostRepository;

    @Autowired private ReactiveCommentsRepository reactiveCommentsRepository;

    private Flux<ReactivePost> reactivePostFlux = null;

    @BeforeEach
    void setUp() {
        reactivePostFlux =
                reactiveCommentsRepository
                        .deleteAll()
                        .then(reactivePostRepository.deleteAll())
                        .thenMany(
                                Flux.just(
                                        ReactivePost.builder()
                                                .title("title 1")
                                                .content("content 1")
                                                .build(),
                                        ReactivePost.builder()
                                                .title("title 2")
                                                .content("content 2")
                                                .build(),
                                        ReactivePost.builder()
                                                .title("title 3")
                                                .content("content 3")
                                                .build()))
                        .flatMap(reactivePostRepository::save)
                        .thenMany(reactivePostRepository.findAll());
    }

    @Test
    void shouldFetchAllReactivePosts() {
        // Fetch all posts using WebClient
        List<ReactivePost> expectedPosts = reactivePostFlux.collectList().block();

        this.webTestClient
                .mutate() // Mutate the client to add basic authentication headers
                .defaultHeaders(headers -> headers.setBasicAuth("user", "password"))
                .build()
                .get()
                .uri("/api/posts/")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(ReactivePost.class)
                .hasSize(expectedPosts.size())
                .isEqualTo(expectedPosts); // Ensure fetched posts match the expected posts
    }

    @Test
    void shouldFindReactivePostById() {
        ReactivePost reactivePost = reactivePostFlux.next().block();
        Long reactivePostId = reactivePost.getId();

        this.webTestClient
                .mutate() // Mutate the client to add basic authentication headers
                .defaultHeaders(
                        headers -> {
                            headers.setBasicAuth("user", "password");
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        })
                .build()
                .get()
                .uri("/api/posts/{id}", reactivePostId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(reactivePostId)
                .jsonPath("$.title")
                .isEqualTo(reactivePost.getTitle())
                .jsonPath("$.content")
                .isEqualTo(reactivePost.getContent());
    }

    @Test
    void shouldCreateNewReactivePost() {
        ReactivePostRequest reactivePost = new ReactivePostRequest("New Title", "New ReactivePost");
        this.webTestClient
                .mutate() // Mutate the client to add basic authentication headers
                .defaultHeaders(
                        headers -> {
                            headers.setBasicAuth("user", "password");
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        })
                .build()
                .post()
                .uri("/api/posts/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(reactivePost))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id")
                .isNotEmpty()
                .jsonPath("$.title")
                .isEqualTo(reactivePost.title())
                .jsonPath("$.content")
                .isEqualTo(reactivePost.content());
    }

    @Test
    void shouldReturn400WhenCreateNewReactivePostWithoutTitleAndContent() {
        ReactivePostRequest reactivePost = new ReactivePostRequest(null, null);

        this.webTestClient
                .mutate() // Mutate the client to add basic authentication headers
                .defaultHeaders(
                        headers -> {
                            headers.setBasicAuth("user", "password");
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        })
                .build()
                .post()
                .uri("/api/posts/")
                .body(BodyInserters.fromValue(reactivePost))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectHeader()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.type")
                .isEqualTo("about:blank")
                .jsonPath("$.title")
                .isEqualTo("Constraint Violation")
                .jsonPath("$.status")
                .isEqualTo(400)
                .jsonPath("$.detail")
                .isEqualTo("Invalid request content.")
                .jsonPath("$.instance")
                .isEqualTo("/api/posts/")
                .jsonPath("$.violations")
                .isArray()
                .jsonPath("$.violations")
                .value(hasSize(2)) // Use .value() with hasSize()
                .jsonPath("$.violations[0].object")
                .isEqualTo("reactivePostRequest")
                .jsonPath("$.violations[0].field")
                .isEqualTo("content")
                .jsonPath("$.violations[0].rejectedValue")
                .isEmpty()
                .jsonPath("$.violations[0].message")
                .isEqualTo("Content must not be blank")
                .jsonPath("$.violations[1].object")
                .isEqualTo("reactivePostRequest")
                .jsonPath("$.violations[1].field")
                .isEqualTo("title")
                .jsonPath("$.violations[1].rejectedValue")
                .isEmpty()
                .jsonPath("$.violations[1].message")
                .isEqualTo("Title must not be blank");
    }

    @Test
    void shouldUpdateReactivePost() {
        ReactivePost reactivePost = reactivePostFlux.next().block();
        Long reactivePostId = reactivePost.getId();
        ReactivePostRequest reactivePostRequest =
                new ReactivePostRequest("Updated ReactivePost", reactivePost.getContent());

        this.webTestClient
                .mutate() // Mutate the client to add basic authentication headers
                .defaultHeaders(
                        headers -> {
                            headers.setBasicAuth("user", "password");
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        })
                .build()
                .put()
                .uri("/api/posts/{id}", reactivePostId)
                .body(BodyInserters.fromValue(reactivePostRequest))
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(reactivePostId)
                .jsonPath("$.title")
                .isEqualTo("Updated ReactivePost");
    }

    @Test
    void shouldDeleteReactivePost() {
        ReactivePost reactivePost = reactivePostFlux.next().block();

        this.webTestClient
                .mutate() // Mutate the client to add basic authentication headers
                .defaultHeaders(headers -> headers.setBasicAuth("admin", "password"))
                .build()
                .delete()
                .uri("/api/posts/{id}", reactivePost.getId())
                .exchange()
                .expectStatus()
                .isNoContent()
                .expectBody()
                .isEmpty();
    }
}
