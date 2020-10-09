package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

public class DemoApplicationIT {

    private WebTestClient client;

    @BeforeEach
    public void setup() {
        this.client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:8080")
                .build();
    }

    @Test
    public void willLoadPosts() {
        this.client.get().uri("/posts")
                .exchange()
                .expectStatus().isOk();
    }

}