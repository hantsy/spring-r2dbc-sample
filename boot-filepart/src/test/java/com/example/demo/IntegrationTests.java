package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTests {

    @LocalServerPort
    private int port;

    private WebTestClient webClient;

    @BeforeEach
    public void setup() {
        this.webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + this.port)
                .build();
    }

    @Test
    public void willLoadPosts() {
        this.webClient.get().uri("/posts")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    public void testUploadAndDownload() {
        var locationUri = this.webClient.post()
                .uri("/posts")
                .bodyValue(new CreatePostCommand("test title", "test content"))
                .exchange()
                .expectHeader().exists("Location")
                .returnResult(ParameterizedTypeReference.forType(Void.class))
                .getRequestHeaders().getLocation();

        assertThat(locationUri).isNotNull();

        var attachmentUri = locationUri + "/attachment";
        this.webClient.put()
                .uri(attachmentUri)
                .bodyValue(generateBody())
                .exchange()
                .expectStatus().isNoContent();

        var responseContent = this.webClient.get()
                .uri(attachmentUri)
                .exchange()
                .expectStatus().isOk()
                .returnResult(ParameterizedTypeReference.forType(byte[].class))
                .getResponseBodyContent();

        assertThat(responseContent).isNotNull();
        assertThat(new String(responseContent)).isEqualTo("test");
    }

    private MultiValueMap<String, HttpEntity<?>> generateBody() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("fileParts", new ClassPathResource("/foo.txt", IntegrationTests.class));
        return builder.build();
    }

}
