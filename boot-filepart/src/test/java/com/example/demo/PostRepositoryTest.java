package com.example.demo;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import reactor.test.StepVerifier;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;
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
    public void testByteBuffer() {
        String s = "testByteBuffer";
        var post = Post.builder().title("r2dbc").attachment(ByteBuffer.wrap(s.getBytes())).build();
        posts.save(post)
                .as(StepVerifier::create)
                .consumeNextWith(saved -> {
                            assertThat(saved.getTitle()).isEqualTo("r2dbc");
                            var attachment = new String(saved.getAttachment().array());
                            assertThat(attachment).isEqualTo(s);
                        }
                )
                .verifyComplete();
    }

    @Test
    public void testByteArray() {
        String s = "testByteBuffer";
        var post = Post.builder().title("r2dbc").coverImage(s.getBytes()).build();
        posts.save(post)
                .as(StepVerifier::create)
                .consumeNextWith(saved -> {
                            assertThat(saved.getTitle()).isEqualTo("r2dbc");
                            var attachment = new String(saved.getCoverImage());
                            assertThat(attachment).isEqualTo(s);
                        }
                )
                .verifyComplete();
    }

}
