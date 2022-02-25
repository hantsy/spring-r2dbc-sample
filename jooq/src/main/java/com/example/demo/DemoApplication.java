package com.example.demo;

import io.r2dbc.spi.ConnectionFactory;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultCloseableDSLContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.*;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.example.demo.jooq.Tables.COMMENTS;
import static com.example.demo.jooq.Tables.POSTS;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

@SpringBootApplication
@EnableR2dbcAuditing
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}

@Configuration
class JooqConfig {

    @Bean
    DSLContext dslContext(ConnectionFactory connectionFactory) {
        return new DefaultCloseableDSLContext(connectionFactory, SQLDialect.POSTGRES);
    }
}


@Configuration
@RequiredArgsConstructor
@Slf4j
class DataInitializer {
    private final DSLContext dslContext;

    @EventListener(classes = ApplicationReadyEvent.class)
    public void init() {
        Mono.from(dslContext.insertInto(POSTS)
                        .columns(POSTS.TITLE, POSTS.CONTENT)
                        .values("jooq test", "content of Jooq test")
                        .returningResult(POSTS.ID)
                )
                .flatMapMany(id -> dslContext.insertInto(COMMENTS)
                        .columns(COMMENTS.POST_ID, COMMENTS.CONTENT)
                        .values(id.component1(), "test comments")
                        .values(id.component1(), "test comments 2")
                )
                .flatMap(it -> dslContext.select(POSTS.TITLE,
                                        POSTS.CONTENT,
                                        multiset(select(COMMENTS.CONTENT)
                                                .from(COMMENTS)
                                                .where(COMMENTS.POST_ID.eq(POSTS.ID))
                                        ).as("comments")
                                )
                                .from(POSTS)
                                .orderBy(POSTS.CREATED_AT)

                )
                .subscribe(
                        data -> log.debug("saving data: {}", data.formatJSON()),
                        error -> log.debug("error: " + error),
                        () -> log.debug("done")
                );
    }
}

interface PostRepository extends R2dbcRepository<Post, UUID> {

    @Query("SELECT * FROM posts where title like :title")
    public Flux<Post> findByTitleContains(String title);
}

interface CommentRepository extends R2dbcRepository<Comment, UUID> {
}

interface HashTagRepository extends R2dbcRepository<HashTag, UUID> {
}

interface PostTagRelationRepository extends R2dbcRepository<PostTagRelation, UUID> {
}

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "posts")
class Post {

    @Id
    @Column("id")
    private UUID id;

    @Column("title")
    private String title;

    @Column("content")
    private String content;

    @Column("status")
    @Builder.Default
    private Status status = Status.DRAFT;

    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column("created_by")
    @CreatedBy
    private String createdBy;

    @Column("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column("version")
    @Version
    private Long version;
}

enum Status {
    DRAFT, PENDING_MODERATION, PUBLISHED;
}

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "comments")
class Comment {

    @Id
    @Column("id")
    private UUID id;

    @Column("content")
    private String content;

    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column("post_id")
    private UUID postId;
}

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "hash_tags")
class HashTag {

    @Id
    @Column("id")
    private UUID id;

    @Column("name")
    private String name;
}

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "post_tags")
class PostTagRelation {

    @Column("post_id")
    private UUID postId;

    @Column("tag_id")
    private UUID tagId;
}