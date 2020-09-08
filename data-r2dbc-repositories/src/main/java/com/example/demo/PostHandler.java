package com.example.demo;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

import static org.springframework.web.reactive.function.server.ServerResponse.*;


/**
 * @author hantsy
 */
@Component
public class PostHandler {

    private final PostRepository posts;

    public PostHandler(PostRepository posts) {
        this.posts = posts;
    }

    public Mono<ServerResponse> all(ServerRequest req) {
        return ok().body(this.posts.findAll(), Post.class);
    }

    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(Post.class)
                .flatMap(this.posts::save)
                .flatMap(post -> created(URI.create("/posts/" + post.getId())).build());
    }

    public Mono<ServerResponse> get(ServerRequest req) {
        return this.posts.findById(UUID.fromString(req.pathVariable("id")))
                .flatMap(post -> ok().body(Mono.just(post), Post.class))
                .switchIfEmpty(notFound().build());
    }

    public Mono<ServerResponse> update(ServerRequest req) {
        var existed = this.posts.findById(UUID.fromString(req.pathVariable("id")));
        return Mono
                .zip(
                        (data) -> {
                            Post p = (Post) data[0];
                            Post p2 = (Post) data[1];
                            if (p2 != null && StringUtils.hasText(p2.getTitle())) {
                                p.setTitle(p2.getTitle());
                            }

                            if (p2 != null && StringUtils.hasText(p2.getContent())) {
                                p.setContent(p2.getContent());
                            }

                            if (p2 != null && p2.getMetadata() != null) {
                                p.setMetadata(p2.getMetadata());
                            }

                            if (p2 != null && p2.getStatus() != null) {
                                p.setStatus(p2.getStatus());
                            }
                            return p;
                        },
                        existed,
                        req.bodyToMono(Post.class)
                )
                .cast(Post.class)
                .flatMap(this.posts::save)
                .flatMap(post -> noContent().build());
    }

    public Mono<ServerResponse> delete(ServerRequest req) {
        return this.posts.deleteById(UUID.fromString(req.pathVariable("id")))
                .flatMap(deleted -> noContent().build());
    }

}