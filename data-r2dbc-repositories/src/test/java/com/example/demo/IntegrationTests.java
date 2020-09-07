package com.example.demo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;

/**
 * @author hantsy
 */
@SpringJUnitConfig(classes = Application.class)
public class IntegrationTests {

    @Value("${server.port:8080}")
    int port;

    WebTestClient client;

    @Autowired
    HttpServer httpServer;

    private DisposableServer disposableServer;

    @BeforeEach
    public void setup() {
        this.disposableServer = this.httpServer.bindNow();
        this.client = WebTestClient
                .bindToServer()
                .responseTimeout(Duration.ofSeconds(5))
                .baseUrl("http://localhost:" + this.port)
                .build();
    }

    @AfterEach
    public void teardown() {
        this.disposableServer.dispose();
    }

    @Test
    public void getAllPostsWillBeOk() throws Exception {
        this.client
                .get().uri("/posts")
                .exchange()
                .expectStatus().isOk();
    }

}
