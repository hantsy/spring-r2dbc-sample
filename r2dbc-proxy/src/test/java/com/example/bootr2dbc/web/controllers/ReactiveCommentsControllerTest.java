package com.example.bootr2dbc.web.controllers;

import static com.example.bootr2dbc.utils.AppConstants.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;

import com.example.bootr2dbc.config.SecurityConfig;
import com.example.bootr2dbc.entities.ReactiveComments;
import com.example.bootr2dbc.model.ReactiveCommentRequest;
import com.example.bootr2dbc.services.ReactiveCommentsService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = ReactiveCommentsController.class)
@ActiveProfiles(PROFILE_TEST)
@WithMockUser(username = "username")
@Import(SecurityConfig.class) // Import the security configuration
class ReactiveCommentsControllerTest {

    @Autowired private WebTestClient webTestClient;

    @MockitoBean private ReactiveCommentsService reactiveCommentsService;

    private Flux<ReactiveComments> reactiveCommentsFlux;

    @BeforeEach
    void setUp() {
        ReactiveComments comment1 =
                ReactiveComments.builder()
                        .id(UUID.randomUUID())
                        .title("First Title")
                        .content("First Content")
                        .postId(1L)
                        .build();
        ReactiveComments comment2 =
                ReactiveComments.builder()
                        .id(UUID.randomUUID())
                        .title("Second Title")
                        .content("Second Content")
                        .postId(1L)
                        .published(true)
                        .publishedAt(LocalDateTime.now())
                        .build();
        ReactiveComments comment3 =
                ReactiveComments.builder()
                        .id(UUID.randomUUID())
                        .title("Third Title")
                        .content("Third Content")
                        .postId(1L)
                        .build();
        reactiveCommentsFlux = Flux.fromIterable(List.of(comment1, comment2, comment3));
    }

    @Test
    void shouldFetchAllReactiveComments() {
        // Fetch all posts using WebClient
        List<ReactiveComments> expectedCommentsList = reactiveCommentsFlux.collectList().block();

        given(reactiveCommentsService.findAllReactiveCommentsByPostId(1L, "title", "asc"))
                .willReturn(reactiveCommentsFlux);

        this.webTestClient
                .get()
                .uri(
                        uriBuilder -> {
                            uriBuilder.queryParam(
                                    "postId", expectedCommentsList.getFirst().getPostId());
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
                .hasSize(expectedCommentsList.size())
                .isEqualTo(expectedCommentsList); // Ensure fetched posts match the expected posts
    }

    @Test
    void shouldFindReactiveCommentsById() {
        ReactiveComments reactiveComments = reactiveCommentsFlux.next().block();
        UUID reactiveCommentsId = reactiveComments.getId();

        given(reactiveCommentsService.findReactiveCommentById(reactiveCommentsId))
                .willReturn(Mono.empty());

        this.webTestClient
                .get()
                .uri("/api/posts/comments/{id}", reactiveCommentsId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .isEmpty();
    }

    @Test
    void shouldReturn404WhenFetchingNonExistingReactiveComments() {
        ReactiveComments reactiveComments = reactiveCommentsFlux.next().block();
        UUID reactiveCommentsId = reactiveComments.getId();

        given(reactiveCommentsService.findReactiveCommentById(reactiveCommentsId))
                .willReturn(Mono.just(reactiveComments));

        this.webTestClient
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
                new ReactiveCommentRequest("First Title", "First Content", reactivePostId, false);

        given(reactiveCommentsService.saveReactiveCommentByPostId(reactiveCommentRequest))
                .willReturn(Mono.just(reactiveComments));

        this.webTestClient
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
        reactiveComments.setTitle("Updated ReactivePost");
        UUID reactivePostId = reactiveComments.getId();
        ReactiveCommentRequest reactivePostRequest =
                new ReactiveCommentRequest(
                        "Updated ReactivePost",
                        reactiveComments.getContent(),
                        reactiveComments.getPostId(),
                        true);

        given(reactiveCommentsService.findReactiveCommentById(reactivePostId))
                .willReturn(Mono.just(reactiveComments));
        given(
                        reactiveCommentsService.updateReactivePostComment(
                                reactivePostRequest, reactiveComments))
                .willReturn(Mono.just(reactiveComments));

        this.webTestClient
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
    void shouldReturn404WhenUpdatingNonExistingReactiveComments() {
        ReactiveComments reactiveComments = reactiveCommentsFlux.next().block();
        UUID reactivePostId = reactiveComments.getId();
        ReactiveCommentRequest reactivePostRequest =
                new ReactiveCommentRequest(
                        "Updated ReactivePost",
                        reactiveComments.getContent(),
                        reactiveComments.getPostId(),
                        false);

        given(reactiveCommentsService.findReactiveCommentById(reactivePostId))
                .willReturn(Mono.empty());

        this.webTestClient
                .put()
                .uri("/api/posts/comments/{id}", reactivePostId)
                .body(BodyInserters.fromValue(reactivePostRequest))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteReactiveComments() {
        ReactiveComments reactiveComments = reactiveCommentsFlux.next().block();

        UUID reactiveCommentsId = reactiveComments.getId();
        given(reactiveCommentsService.findReactiveCommentById(reactiveCommentsId))
                .willReturn(Mono.just(reactiveComments));
        given(reactiveCommentsService.deleteReactiveCommentById(reactiveCommentsId))
                .willReturn(Mono.empty());

        this.webTestClient
                .delete()
                .uri("/api/posts/comments/{id}", reactiveCommentsId)
                .exchange()
                .expectStatus()
                .isNoContent()
                .expectBody()
                .isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404WhenDeletingNonExistingReactiveComments() {
        ReactiveComments reactiveComments = reactiveCommentsFlux.next().block();

        UUID reactiveCommentsId = reactiveComments.getId();
        given(reactiveCommentsService.findReactiveCommentById(reactiveCommentsId))
                .willReturn(Mono.empty());
        this.webTestClient
                .delete()
                .uri("/api/posts/comments/{id}", reactiveCommentsId)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .isEmpty();
    }

    @Test
    void shouldReturn404WhenDeletingReactivePostComment() {
        this.webTestClient
                .delete()
                .uri("/api/posts/comments/{id}", 10_000L)
                .exchange()
                .expectStatus()
                .isForbidden();
    }
}
