package com.example.demo;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@DataR2dbcTest()
@Slf4j
public class BookRepositoryTest {
    @Container
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer<>("postgres:12");
    //.withCopyFileToContainer(MountableFile.forClasspathResource("init.sql"), "/docker-entrypoint-initdb.d/init.sql");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://"
                + postgreSQLContainer.getHost() + ":" + postgreSQLContainer.getFirstMappedPort()
                + "/" + postgreSQLContainer.getDatabaseName());
        registry.add("spring.r2dbc.username", () -> postgreSQLContainer.getUsername());
        registry.add("spring.r2dbc.password", () -> postgreSQLContainer.getPassword());
    }

    @TestConfiguration
    @Import(R2dbcConfig.class)
    @ImportAutoConfiguration(JacksonAutoConfiguration.class)
    static class TestConfig {

    }

    @Autowired
    R2dbcEntityTemplate template;

    @Autowired
    BookRepository books;

    @SneakyThrows
    @BeforeEach
    public void setup() {
        var latch = new CountDownLatch(1);

        this.template.delete(Book.class).all()
                .doOnTerminate(latch::countDown)
                .subscribe(data -> log.debug("delete books."));

        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void testDatabaseClientExisted() {
        assertNotNull(template);
    }

    @Test
    public void testRepositoryExisted() {
        assertNotNull(books);
    }

    @Test
    public void testInsertAndQuery() {
        var entity = Book.builder()
                .title("test title")
                .description("content of test")
                .tags(List.of("Spring", "R2dbc"))
                .metadata(Map.of("test", "test"))
                .build();
        this.template.insert(entity)
                .log()
                .then()
                .thenMany(
                        this.books.findByTitleContains("test%")
                )
                .log()
                .as(StepVerifier::create)
                .consumeNextWith(p -> {
                    assertEquals("test title", p.getTitle());
                })
                .verifyComplete();

    }

}
