package com.example.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.ok;

@SpringBootApplication
@Slf4j
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
class PostController {
    private final PostRepository postRepository;

    @GetMapping("")
    public Flux<Post> all() {
       return  this.postRepository.findAll();
    }

    @GetMapping("{id}")
    public ResponseEntity<Mono<Post>> get(@PathVariable UUID id) {
        var post = this.postRepository.findById(id);
        return ok( post);
    }

}


@Configuration
@EnableR2dbcAuditing
class DataConfig {

    @Bean
    ReactiveAuditorAware<String> auditorAware() {
        return () -> Mono.just("test");
    }
}


interface PostRepository extends R2dbcRepository<Post, UUID> {
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

        @Column("created_at")
        @CreatedDate
        LocalDateTime createdAt,

        @Column("created_by")
        @CreatedBy
        String createdBy,

        @Column("updated_at")
        @LastModifiedDate
        LocalDateTime updatedAt,

        @Column("updated_by")
        @LastModifiedBy
        String updatedBy,

        @Column("version")
        @Version
        Long version
) {

    public static Post of(String title, String content) {
        return new Post(null, title, content, null, null, null, null, null, null);
    }

    enum Status {
        DRAFT, PENDING_MODERATION, PUBLISHED;
    }

}


