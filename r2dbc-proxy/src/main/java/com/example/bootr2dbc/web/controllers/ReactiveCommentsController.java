package com.example.bootr2dbc.web.controllers;

import com.example.bootr2dbc.entities.ReactiveComments;
import com.example.bootr2dbc.model.ReactiveCommentRequest;
import com.example.bootr2dbc.services.ReactiveCommentsService;
import com.example.bootr2dbc.utils.AppConstants;
import java.net.URI;
import java.util.UUID;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/posts/comments")
@RequiredArgsConstructor
public class ReactiveCommentsController {

    private final ReactiveCommentsService reactiveCommentsService;

    @GetMapping("/")
    public Flux<ReactiveComments> getAllReactiveComments(
            @RequestParam Long postId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY, required = false)
                    String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false)
                    String sortDir) {
        return reactiveCommentsService.findAllReactiveCommentsByPostId(postId, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ReactiveComments>> getReactiveCommentsById(@PathVariable UUID id) {
        return reactiveCommentsService
                .findReactiveCommentById(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping("/")
    public Mono<ResponseEntity<ReactiveComments>> createReactiveComment(
            @RequestBody @Validated ReactiveCommentRequest reactiveCommentRequest,
            UriComponentsBuilder uriComponentsBuilder) {
        return reactiveCommentsService
                .saveReactiveCommentByPostId(reactiveCommentRequest)
                .map(
                        savedPostComment -> {
                            // Build the location URI
                            String location =
                                    uriComponentsBuilder
                                            .path("/api/post/comments/{id}")
                                            .buildAndExpand(savedPostComment.getId())
                                            .toUriString();

                            // Create a ResponseEntity with the Location header and the saved
                            // ReactivePost
                            return ResponseEntity.created(URI.create(location))
                                    .body(savedPostComment);
                        });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ReactiveComments>> updateReactiveComment(
            @PathVariable UUID id, @RequestBody ReactiveCommentRequest reactiveCommentRequest) {
        return reactiveCommentsService
                .findReactiveCommentById(id)
                .flatMap(
                        existingPostComment ->
                                reactiveCommentsService
                                        .updateReactivePostComment(
                                                reactiveCommentRequest, existingPostComment)
                                        .map(ResponseEntity::ok))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteReactiveComment(@PathVariable UUID id) {
        return reactiveCommentsService
                .findReactiveCommentById(id)
                .flatMap(
                        reactivePostComment ->
                                reactiveCommentsService
                                        .deleteReactiveCommentById(id)
                                        .then(Mono.just(ResponseEntity.noContent().<Void>build())))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
}
