package com.example.demo;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Component
class BookHandler {

    private final BookRepository posts;

    public BookHandler(BookRepository posts) {
        this.posts = posts;
    }

    public Mono<ServerResponse> all(ServerRequest req) {
        return ok().body(this.posts.findAll(), Book.class);
    }

    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(Book.class)
                .flatMap(this.posts::save)
                .flatMap(post -> created(URI.create("/books/" + post.getId())).build());
    }

    public Mono<ServerResponse> get(ServerRequest req) {
        return this.posts.findById(UUID.fromString(req.pathVariable("id")))
                .flatMap(post -> ok().body(Mono.just(post), Book.class))
                .switchIfEmpty(notFound().build());
    }

    public Mono<ServerResponse> update(ServerRequest req) {
        var existed = this.posts.findById(UUID.fromString(req.pathVariable("id")));
        return Mono
                .zip(
                        (data) -> {
                            Book p = (Book) data[0];
                            Book p2 = (Book) data[1];
                            p.setTitle(p2.getTitle());
                            p.setDescription(p2.getDescription());
                            return p;
                        },
                        existed,
                        req.bodyToMono(Book.class)
                )
                .cast(Book.class)
                .flatMap(this.posts::save)
                .flatMap(post -> noContent().build());
    }

    public Mono<ServerResponse> delete(ServerRequest req) {
        return this.posts.deleteById(UUID.fromString(req.pathVariable("id")))
                .flatMap(deleted -> noContent().build());
    }
}
