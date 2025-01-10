package com.example.bootr2dbc.web.controllers;

import com.example.bootr2dbc.entities.ReactiveComments;
import com.example.bootr2dbc.entities.ReactivePost;
import com.example.bootr2dbc.model.ReactivePostRequest;
import com.example.bootr2dbc.services.ReactivePostService;
import com.example.bootr2dbc.utils.AppConstants;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class ReactivePostController {

    private final ReactivePostService reactivePostService;

    @GetMapping("/")
    public Mono<ResponseEntity<List<ReactivePost>>> getAllReactivePosts(
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY, required = false)
                    String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false)
                    String sortDir) {
        return reactivePostService
                .findAllReactivePosts(sortBy, sortDir)
                .collectList()
                .flatMap(
                        posts -> {
                            if (posts.isEmpty()) {
                                return Mono.just(ResponseEntity.noContent().build());
                            } else {
                                return Mono.just(ResponseEntity.ok(posts));
                            }
                        });
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ReactivePost>> getReactivePostById(@PathVariable Long id) {
        return reactivePostService
                .findReactivePostById(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/{postId}/comments")
    public Mono<ResponseEntity<List<ReactiveComments>>> getCommentsForReactivePost(
            @PathVariable Long postId) {
        return reactivePostService
                .findCommentsForReactivePost(postId)
                .collectList()
                .flatMap(
                        comments -> {
                            if (comments.isEmpty()) {
                                return Mono.just(ResponseEntity.noContent().build());
                            } else {
                                return Mono.just(ResponseEntity.ok(comments));
                            }
                        });
    }

    @PostMapping("/")
    public Mono<ResponseEntity<ReactivePost>> createReactivePost(
            @RequestBody @Validated ReactivePostRequest reactivePostRequest,
            UriComponentsBuilder uriComponentsBuilder) {
        return reactivePostService
                .saveReactivePost(reactivePostRequest)
                .map(
                        savedPost -> {
                            // Build the location URI
                            String location =
                                    uriComponentsBuilder
                                            .path("/api/posts/{id}")
                                            .buildAndExpand(savedPost.getId())
                                            .toUriString();

                            // Create a ResponseEntity with the Location header and the saved
                            // ReactivePost
                            return ResponseEntity.created(URI.create(location)).body(savedPost);
                        });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ReactivePost>> updateReactivePost(
            @PathVariable Long id,
            @Validated @RequestBody ReactivePostRequest reactivePostRequest) {
        return reactivePostService
                .findReactivePostById(id)
                .flatMap(
                        existingPost ->
                                reactivePostService
                                        .updateReactivePost(reactivePostRequest, existingPost)
                                        .map(ResponseEntity::ok))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> deleteReactivePost(@PathVariable Long id) {
        return reactivePostService.deleteReactivePostAndCommentsById(id);
    }
}
