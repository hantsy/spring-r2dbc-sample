package com.example.bootr2dbc.web.controllers;

import static com.example.bootr2dbc.utils.AppConstants.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.example.bootr2dbc.config.SecurityConfig;
import com.example.bootr2dbc.entities.ReactivePost;
import com.example.bootr2dbc.model.ReactivePostRequest;
import com.example.bootr2dbc.services.ReactivePostService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = ReactivePostController.class)
@ActiveProfiles(PROFILE_TEST)
@WithMockUser(username = "username")
@Import(SecurityConfig.class) // Import the security configuration
class ReactivePostControllerTest {

    @Autowired private WebTestClient webTestClient;

    @MockitoBean private ReactivePostService reactivePostService;

    private Flux<ReactivePost> reactivePostFlux;

    @BeforeEach
    void setUp() {
        List<ReactivePost> reactivePostList = new ArrayList<>();
        reactivePostList.add(
                ReactivePost.builder().id(1L).title("title 1").content("content 1").build());
        reactivePostList.add(
                ReactivePost.builder().id(2L).title("title 2").content("content 2").build());
        reactivePostList.add(
                ReactivePost.builder().id(3L).title("title 3").content("content 3").build());
        reactivePostFlux = Flux.fromIterable(reactivePostList);
    }

    @Test
    void shouldFetchAllReactivePosts() {
        // Fetch all posts using WebClient
        List<ReactivePost> expectedPosts = reactivePostFlux.collectList().block();
        assertThat(expectedPosts).isNotEmpty().hasSize(3);

        given(reactivePostService.findAllReactivePosts("id", "asc")).willReturn(reactivePostFlux);

        this.webTestClient
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
    void shouldReturn404FetchAllReactivePosts() {
        given(reactivePostService.findAllReactivePosts("id", "asc")).willReturn(Flux.empty());

        this.webTestClient
                .get()
                .uri("/api/posts/")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    void shouldFindReactivePostById() {

        ReactivePost reactivePost = reactivePostFlux.next().block();
        Long reactivePostId = reactivePost.getId();
        given(reactivePostService.findReactivePostById(reactivePostId))
                .willReturn(Mono.just(reactivePost));

        this.webTestClient
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
    void shouldReturn404WhenFetchingNonExistingReactivePost() {
        Long reactivePostId = 1L;
        given(reactivePostService.findReactivePostById(reactivePostId)).willReturn(Mono.empty());

        this.webTestClient
                .get()
                .uri("/api/posts/{id}", reactivePostId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .isEmpty();
    }

    @Test
    void shouldCreateNewReactivePost() {

        ReactivePostRequest reactivePost = new ReactivePostRequest("title 1", "content 1");
        given(reactivePostService.saveReactivePost(reactivePost))
                .willReturn(reactivePostFlux.next());

        this.webTestClient
                .mutateWith(csrf())
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
                .mutateWith(csrf())
                .post()
                .uri("/api/posts/")
                .contentType(MediaType.APPLICATION_JSON)
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
        reactivePost.setTitle("Updated ReactivePost");

        given(reactivePostService.findReactivePostById(reactivePostId))
                .willReturn(Mono.just(reactivePost));
        given(reactivePostService.updateReactivePost(reactivePostRequest, reactivePost))
                .willReturn(Mono.just(reactivePost));

        this.webTestClient
                .mutateWith(csrf())
                .put()
                .uri("/api/posts/{id}", reactivePostId)
                .contentType(MediaType.APPLICATION_JSON)
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
    void shouldReturn404WhenUpdatingNonExistingReactivePost() {

        ReactivePostRequest reactivePostRequest =
                new ReactivePostRequest("Updated ReactivePost", "Updated Content");
        Long reactivePostId = 1000L;
        given(reactivePostService.findReactivePostById(reactivePostId)).willReturn(Mono.empty());

        this.webTestClient
                .mutateWith(csrf())
                .put()
                .uri("/api/posts/{id}", reactivePostId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(reactivePostRequest))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .isEmpty();
    }

    @Test
    @WithMockUser(
            username = "admin",
            roles = {"USER", "ADMIN"})
    void shouldDeleteReactivePost() {
        Long reactivePostId = 1L;
        ReactivePost reactivePost = reactivePostFlux.next().block();
        given(reactivePostService.deleteReactivePostAndCommentsById(reactivePostId))
                .willReturn(Mono.just(ResponseEntity.noContent().build()));

        this.webTestClient
                .delete()
                .uri("/api/posts/{id}", reactivePost.getId())
                .exchange()
                .expectStatus()
                .isNoContent()
                .expectBody()
                .isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404WhenDeletingNonExistingReactivePost() {
        Long reactivePostId = 1L;
        ReactivePost reactivePost = reactivePostFlux.next().block();
        given(reactivePostService.deleteReactivePostAndCommentsById(reactivePostId))
                .willReturn(Mono.just(ResponseEntity.notFound().build()));

        this.webTestClient
                .delete()
                .uri("/api/posts/{id}", reactivePost.getId())
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void shouldReturn404WhenDeletingReactivePost() {
        this.webTestClient
                .delete()
                .uri("/api/posts/{id}", 10_000L)
                .exchange()
                .expectStatus()
                .isForbidden();
    }
}
