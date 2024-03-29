package com.example.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/posts")
@Slf4j
@RequiredArgsConstructor
class PostController {

    private final PostRepository posts;

    @GetMapping
    public Flux<PostSummary> all(@RequestParam(required = true, defaultValue = "") String title,
                                 @RequestParam(required = true, defaultValue = "0") Integer page,
                                 @RequestParam(required = true, defaultValue = "10") Integer size
    ) {
        return posts.findByTitleLike("%" + title + "%", PageRequest.of(page, size));
    }

    @PostMapping
    public Mono<ResponseEntity> create(@RequestBody CreatePostCommand data) {
        return posts.save(Post.builder().title(data.title()).content(data.content()).build())
                .map(saved -> ResponseEntity.created(URI.create("/posts/" + saved.getId())).build());
    }

    @GetMapping("{id}")
    public Mono<ResponseEntity<Post>> get(@PathVariable UUID id) {
        return this.posts.findById(id)
                .map(post -> ResponseEntity.ok().body(post))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PutMapping("{id}/attachment")
    public Mono<ResponseEntity> upload(@PathVariable UUID id,
                                          @RequestPart Mono<FilePart> fileParts) {

        return Mono
                .zip(objects -> {
                            var post = (Post) objects[0];
                            var filePart = (DataBuffer) objects[1];
                            post.setAttachment(filePart.toByteBuffer());
                            return post;
                        },
                        this.posts.findById(id),
                        fileParts.flatMap(filePart -> DataBufferUtils.join(filePart.content()))
                )
                .flatMap(this.posts::save)
                .map(saved -> ResponseEntity.noContent().build());

    }


    @GetMapping("{id}/attachment")
    public Mono<Void> read(@PathVariable UUID id, ServerWebExchange exchange) {
        return this.posts.findById(id)
                .log()
                .map(post -> Mono.just(new DefaultDataBufferFactory().wrap(post.getAttachment())))
                .flatMap(r -> exchange.getResponse().writeWith(r));
    }

}
