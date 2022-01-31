package com.example.demo;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataR2dbcTest()
@Slf4j
@ContextConfiguration(initializers = PostRepositoryWithCustomContextInitializerTest.TestContainerInitializer.class)
public class PostRepositoryWithCustomContextInitializerTest {

    static class TestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer<>("postgres:12")
                    .withCopyFileToContainer(MountableFile.forClasspathResource("init.sql"), "/docker-entrypoint-initdb.d/init.sql");
            postgreSQLContainer.start();
            log.info(" container.getFirstMappedPort():: {}", postgreSQLContainer.getFirstMappedPort());
            configurableApplicationContext
                    .addApplicationListener((ApplicationListener<ContextClosedEvent>) event -> postgreSQLContainer.stop());
            TestPropertyValues
                    .of(
                            "spring.r2dbc.url=" +  "r2dbc:postgresql://"
                                    + postgreSQLContainer.getHost() + ":" + postgreSQLContainer.getFirstMappedPort()
                                    + "/" + postgreSQLContainer.getDatabaseName(),
                            "spring.r2dbc.username=" + postgreSQLContainer.getUsername(),
                            "spring.r2dbc.password=" + postgreSQLContainer.getPassword()
                    )
                    .applyTo(configurableApplicationContext);
        }
    }

    @Autowired
    R2dbcEntityTemplate template;

    @Autowired
    PostRepository posts;

    @BeforeEach
    public void setup() {
        this.template.delete(Post.class).all().block(Duration.ofSeconds(5));
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
    public void testInsertAndQuery() {
        var data = Post.builder().title("test title").content("content of test").build();
        this.template.insert(data)
                .thenMany(
                        this.posts.findByTitleContains("test%")
                )
                .log()
                .as(StepVerifier::create)
                .consumeNextWith(p -> {
                            log.info("saved post: {}", p);
                            assertThat(p.getTitle()).isEqualTo("test title");
                        }
                )
                .verifyComplete();

    }


}
