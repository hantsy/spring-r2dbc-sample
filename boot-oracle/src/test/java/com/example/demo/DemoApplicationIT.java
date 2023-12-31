package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class DemoApplicationIT {

    private WebTestClient client;

    @BeforeEach
    public void setup() {
        this.client = WebTestClient.bindToServer()
            .baseUrl("http://localhost:8080")
            .build();
    }

    @Test
    public void testGetAllPosts() {
        this.client.get().uri("/todos")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.size()").isEqualTo(2);
    }

}
