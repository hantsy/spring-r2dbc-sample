package com.example.bootr2dbc.web.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;

import com.example.bootr2dbc.common.AbstractIntegrationTest;
import com.example.bootr2dbc.entities.ReactiveComments;
import com.example.bootr2dbc.entities.ReactivePost;
import com.example.bootr2dbc.model.ReactiveCommentRequest;
import com.example.bootr2dbc.repositories.ReactiveCommentsRepository;
import com.example.bootr2dbc.repositories.ReactivePostRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class ReactiveCommentsControllerIT extends AbstractIntegrationTest {

    @Autowired private ReactiveCommentsRepository reactiveCommentsRepository;

    @Autowired private ReactivePostRepository reactivePostRepository;

    private Flux<ReactiveComments> reactiveCommentsFlux;

    @BeforeEach
    void setUp() {
        reactiveCommentsFlux =
                reactiveCommentsRepository
                        .deleteAll()
                        .then(reactivePostRepository.deleteAll())
                        .then(
                                reactivePostRepository
                                        .save(
                                                ReactivePost.builder()
                                                        .title("title 1")
                                                        .content("content 1")
                                                        .build())
                                        .flatMap(
                                                reactivePost -> {
                                                    ReactiveComments comment1 =
                                                            ReactiveComments.builder()
                                                                    .title("First Title")
                                                                    .content("First Content")
                                                                    .postId(reactivePost.getId())
                                                                    .build();
                                                    ReactiveComments comment2 =
                                                            ReactiveComments.builder()
                                                                    .title("Second Title")
                                                                    .content("Second Content")
                                                                    .postId(reactivePost.getId())
                                                                    .published(true)
                                                                    .publishedAt(
                                                                            LocalDateTime.now())
                                                                    .build();
                                                    ReactiveComments comment3 =
                                                            ReactiveComments.builder()
                                                                    .title("Third Title")
                                                                    .content("Third Content")
                                                                    .postId(reactivePost.getId())
                                                                    .build();

                                                    return reactiveCommentsRepository
                                                            .save(comment1)
                                                            .then(
                                                                    reactiveCommentsRepository.save(
                                                                            comment2))
                                                            .then(
                                                                    reactiveCommentsRepository.save(
                                                                            comment3))
                                                            .then(Mono.just(reactivePost));
                                                }))
                        .flatMapMany(
                                post -> reactiveCommentsRepository.findAllByPostId(post.getId()));
    }

    @Test
    void shouldFetchAllReactiveComments() {
        // Fetch all posts using WebClient
        List<ReactiveComments> expectedPostComments = reactiveCommentsFlux.collectList().block();

        this.webTestClient
                .mutate() // Mutate the client to add basic authentication headers
                .defaultHeaders(headers -> headers.setBasicAuth("user", "password"))
                .build()
                .get()
                .uri(
                        uriBuilder -> {
                            uriBuilder.queryParam(
                                    "postId", expectedPostComments.getFirst().getPostId());
                            uriBuilder.queryParam("sortBy", "title");
                            uriBuilder.path("/api/posts/comments/");
                            return uriBuilder.build();
                        })
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(ReactiveComments.class)
                .hasSize(expectedPostComments.size())
                .isEqualTo(expectedPostComments); // Ensure fetched comments match the expected
        // comments
    }

    @Test
    void shouldFindReactiveCommentsById() {
        ReactiveComments reactiveComments = reactiveCommentsFlux.next().block();
        UUID reactiveCommentsId = reactiveComments.getId();

        this.webTestClient
                .mutate() // Mutate the client to add basic authentication headers
                .defaultHeaders(
                        headers -> {
                            headers.setBasicAuth("user", "password");
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        })
                .build()
                .get()
                .uri("/api/posts/comments/{id}", reactiveCommentsId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(reactiveCommentsId.toString())
                .jsonPath("$.title")
                .isEqualTo(reactiveComments.getTitle())
                .jsonPath("$.content")
                .isEqualTo(reactiveComments.getContent());
    }

    @Test
    void shouldCreateNewReactiveComments() {
        ReactiveComments reactiveComments = reactiveCommentsFlux.next().block();
        Long reactivePostId = reactiveComments.getPostId();
        ReactiveCommentRequest reactiveCommentRequest =
                new ReactiveCommentRequest(
                        "New Title", "New ReactiveComments", reactivePostId, false);
        this.webTestClient
                .mutate() // Mutate the client to add basic authentication headers
                .defaultHeaders(
                        headers -> {
                            headers.setBasicAuth("user", "password");
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        })
                .build()
                .post()
                .uri("/api/posts/comments/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(reactiveCommentRequest))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id")
                .value(
                        returningId -> {
                            // Attempt to parse the value as a UUID
                            UUID uuid = UUID.fromString((String) returningId);
                            assertThat(uuid).isNotNull();
                        })
                .jsonPath("$.title")
                .isEqualTo(reactiveCommentRequest.title())
                .jsonPath("$.content")
                .isEqualTo(reactiveCommentRequest.content())
                .jsonPath("$.postId")
                .isEqualTo(reactivePostId)
                .jsonPath("$.published")
                .isEqualTo(false);
    }

    @Test
    void shouldReturn400WhenCreateNewReactiveCommentsWithoutTitleAndContent() {
        ReactiveCommentRequest reactiveCommentRequest =
                new ReactiveCommentRequest(null, null, -90L, false);

        this.webTestClient
                .mutate() // Mutate the client to add basic authentication headers
                .defaultHeaders(
                        headers -> {
                            headers.setBasicAuth("user", "password");
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        })
                .build()
                .post()
                .uri("/api/posts/comments/")
                .body(BodyInserters.fromValue(reactiveCommentRequest))
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
                .isEqualTo("/api/posts/comments/")
                .jsonPath("$.violations")
                .isArray()
                .jsonPath("$.violations")
                .value(hasSize(3)) // Use .value() with hasSize()
                .jsonPath("$.violations[0].object")
                .isEqualTo("reactiveCommentRequest")
                .jsonPath("$.violations[0].field")
                .isEqualTo("content")
                .jsonPath("$.violations[0].rejectedValue")
                .isEmpty()
                .jsonPath("$.violations[0].message")
                .isEqualTo("Content must not be blank")
                .jsonPath("$.violations[1].object")
                .isEqualTo("reactiveCommentRequest")
                .jsonPath("$.violations[1].field")
                .isEqualTo("postId")
                .jsonPath("$.violations[1].rejectedValue")
                .isEqualTo(-90)
                .jsonPath("$.violations[1].message")
                .isEqualTo("PostId must be greater than 0")
                .jsonPath("$.violations[2].object")
                .isEqualTo("reactiveCommentRequest")
                .jsonPath("$.violations[2].field")
                .isEqualTo("title")
                .jsonPath("$.violations[2].rejectedValue")
                .isEmpty()
                .jsonPath("$.violations[2].message")
                .isEqualTo("Title must not be blank");
    }

    @Test
    void shouldUpdateReactiveComments() {
        ReactiveComments reactiveComments = reactiveCommentsFlux.next().block();
        UUID reactivePostId = reactiveComments.getId();
        ReactiveCommentRequest reactivePostRequest =
                new ReactiveCommentRequest(
                        "Updated ReactivePost",
                        reactiveComments.getContent(),
                        reactiveComments.getPostId(),
                        true);

        this.webTestClient
                .mutate() // Mutate the client to add basic authentication headers
                .defaultHeaders(
                        headers -> {
                            headers.setBasicAuth("user", "password");
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        })
                .build()
                .put()
                .uri("/api/posts/comments/{id}", reactivePostId)
                .body(BodyInserters.fromValue(reactivePostRequest))
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(reactivePostId.toString())
                .jsonPath("$.title")
                .isEqualTo("Updated ReactivePost");
    }

    @Test
    void shouldDeleteReactiveComments() {
        ReactiveComments reactiveComments = reactiveCommentsFlux.next().block();

        this.webTestClient
                .mutate() // Mutate the client to add basic authentication headers
                .defaultHeaders(headers -> headers.setBasicAuth("admin", "password"))
                .build()
                .delete()
                .uri("/api/posts/comments/{id}", reactiveComments.getId())
                .exchange()
                .expectStatus()
                .isNoContent()
                .expectBody()
                .isEmpty();
    }
}
