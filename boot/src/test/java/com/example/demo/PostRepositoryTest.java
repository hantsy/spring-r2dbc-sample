package com.example.demo;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import reactor.test.StepVerifier;

import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataR2dbcTest()
@Testcontainers
@Slf4j
public class PostRepositoryTest {

    @Container
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer<>("postgres:12")
            .withCopyFileToContainer(MountableFile.forClasspathResource("init.sql"), "/docker-entrypoint-initdb.d/init.sql");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://"
                + postgreSQLContainer.getHost() + ":" + postgreSQLContainer.getFirstMappedPort()
                + "/" + postgreSQLContainer.getDatabaseName());
        registry.add("spring.r2dbc.username", () -> postgreSQLContainer.getUsername());
        registry.add("spring.r2dbc.password", () -> postgreSQLContainer.getPassword());
    }

    @Autowired
    R2dbcEntityTemplate template;

    @Autowired
    PostRepository posts;

    @BeforeEach
    public void setup() {

    }

    @Test
    public void testDatabaseClientExisted() {
        assertNotNull(template);
    }

    @Test
    public void testPostRepositoryExisted() {
        assertNotNull(posts);
    }

    @Test
    public void testQueryByExample() {
        var post = Post.builder().title("r2dbc").build();
        var exampleMatcher = ExampleMatcher.matching().withMatcher("title", matcher -> matcher.ignoreCase().contains());
        var example = Example.of(post, exampleMatcher);
        var data = posts.findBy(example, postReactiveFluentQuery -> postReactiveFluentQuery.page(PageRequest.of(0, 10)));

        StepVerifier.create(data)
                .consumeNextWith(p -> {
                    log.debug("post data: {}", p.getContent());
                    assertThat( p.getTotalElements()).isEqualTo(1);
                })
                .verifyComplete();
    }

    @Test
    public void testInsertAndQuery() {
        this.template.insert(Post.builder().title("test title").content("content of test").build())
                .log()
                .then()
                .thenMany(
                        this.posts.findByTitleContains("test%")
                )
                .log()
                .as(StepVerifier::create)
                .consumeNextWith(p -> {
                    assertEquals("test title", p.getTitle());
                    assertNotNull(p.getCreatedAt());
                    assertNotNull(p.getUpdatedAt());
                })
                .verifyComplete();

    }

    @Test
    public void testInsertAndCount() {
        this.template.insert(Post.builder().title("test title").content("content of test").build())
                .log()
                .then()
                .then(
                        this.posts.countByTitleContaining("test")
                )
                .log()
                .as(StepVerifier::create)
                .consumeNextWith(p -> {
                    assertThat(p).isEqualTo(1);
                })
                .verifyComplete();

    }

    @Test
    public void testInsertAndFindByTitleLike() {
        var data = IntStream.range(1, 101)
                .mapToObj(
                        i -> Post.builder().title("test title#" + i).content("content of test").build()
                )
                .collect(toList());
        this.posts.saveAll(data)
                .log()
                .then()
                .thenMany(
                        this.posts.findByTitleLike("test%", PageRequest.of(0, 10))
                )
                .log()
                .as(StepVerifier::create)
                .expectNextCount(10)
                .verifyComplete();

    }
}
