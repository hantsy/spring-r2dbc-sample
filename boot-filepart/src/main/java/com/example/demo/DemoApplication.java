package com.example.demo;

import io.r2dbc.spi.Blob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.UUID;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

@Table(value = "posts")
record Post(

        @Id
        @Column("id")
        UUID id,

        @Column("title")
        String title,

        @Column("content")
        String content,

        @Column("status")
        Status status,

        @Column("attachment")
        ByteBuffer attachment,

        @Column("cover_image")
        byte[] coverImage,

        @Column("cover_image_thumbnail")
        Blob coverImageThumbnail,

        @Column("version")
        @Version
        Long version
) {

    public static Post of(String title, String content) {
        return new Post(null, title, content, null, null, null, null, null);
    }

    public static Post of(UUID id, String title, String content) {
        return new Post(id, title, content, null, null, null, null, null);
    }

    public Post withAttachment(ByteBuffer attachment) {
        return new Post(this.id, this.title, this.content, this.status, attachment, this.coverImage, this.coverImageThumbnail, this.version);
    }

    public Post withCoverImage(byte[] bytes) {
        return new Post(this.id, this.title, this.content, this.status, this.attachment, bytes, this.coverImageThumbnail, this.version);
    }

    public Post withCoverImageThumbnail(Blob bytes) {
        return new Post(this.id, this.title, this.content, this.status, this.attachment, this.coverImage, bytes, this.version);
    }

    enum Status {
        DRAFT, PENDING_MODERATION, PUBLISHED;
    }

}

record PostSummary(UUID id, String title) {
}

interface PostRepository extends R2dbcRepository<Post, UUID> {
    public Flux<PostSummary> findByTitleLike(String title, Pageable pageable);
}

record CreatePostCommand(String title, String content) {
}

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
        return posts.save(Post.of(data.title(), data.content()))
                .map(saved -> ResponseEntity.created(URI.create("/posts/" + saved.id())).build());
    }

    @GetMapping("{id}")
    public Mono<ResponseEntity<Post>> get(@PathVariable UUID id) {
        return this.posts.findById(id)
                .map(post -> ResponseEntity.ok().body(post))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PutMapping("{id}/attachment")
    public Mono<ResponseEntity<?>> upload(@PathVariable UUID id,
                                       @RequestPart Mono<FilePart> fileParts) {

        return Mono.zip(objects -> {
                            var post = (Post) objects[0];
                            var dataBuffer = (DataBuffer) objects[1];

                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer); // Release the buffer after use

                            var postWithAttachment = post.withAttachment(ByteBuffer.wrap(bytes));
                            return postWithAttachment;
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
                .map(post -> Mono.just(new DefaultDataBufferFactory().wrap(post.attachment())))
                .flatMap(r -> exchange.getResponse().writeWith(r));
    }

}
