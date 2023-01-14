package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTests {

    @LocalServerPort
    private int port;

    private WebClient webClient;

    @BeforeEach
    public void setup() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:" + this.port)
                .build();
    }

    @Test
    public void willLoadPosts() {
        var verifier = webClient.get().uri("messages")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(Message.class)
                .log()
                .as(StepVerifier::create)
                .consumeNextWith(it -> assertThat(it.body()).isEqualTo("test message"))
                //.consumeNextWith(it -> assertThat(it.body()).isEqualTo("test message2"))
                .thenCancel()
                .verifyLater();
        webClient.post().uri("messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateMessageCommand("test message"))
                .retrieve().toBodilessEntity()
                .then()
                .block();
//        webClient.post().uri("messages")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(new CreateMessageCommand("test message2"))
//                .retrieve().toBodilessEntity()
//                .then()
//                .block();

        verifier.verify(Duration.ofMillis(500));
    }

}
