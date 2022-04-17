package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

import static org.springframework.web.reactive.function.server.ServerResponse.created;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
@RequiredArgsConstructor
class ReviewHandler {

    private final ReviewRepository reviewRepository;

    public Mono<ServerResponse> create(ServerRequest req) {
        var bookId = UUID.fromString(req.pathVariable("id"));
        return req.bodyToMono(Review.class)
                .map(comment -> {
                    comment.setBookId(bookId);
                    return comment;
                })
                .flatMap(this.reviewRepository::save)
                .flatMap(c -> created(URI.create("/books/" + bookId + "/reviews/" + c.getId())).build());
    }

    public Mono<ServerResponse> getByBookId(ServerRequest req) {
        var result = this.reviewRepository.findByBookId(UUID.fromString(req.pathVariable("id")));
        return ok().body(result, Review.class);
    }
}
