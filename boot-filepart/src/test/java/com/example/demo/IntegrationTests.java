package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
@Import(TestcontainersConfiguration.class)
public class IntegrationTests {

    @LocalServerPort
    private int port;

    private WebTestClient webClient;

    @BeforeEach
    public void setup() {
        this.webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + this.port)
                .codecs(clientCodecConfigurer ->
                        clientCodecConfigurer.defaultCodecs().enableLoggingRequestDetails(true)
                )
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
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreatePostCommand("test title", "test content"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .returnResult(ParameterizedTypeReference.forType(Void.class))
                .getResponseHeaders().getLocation();

        log.debug("location uri: {}", locationUri);
        assertThat(locationUri).isNotNull();

        var attachmentUri = locationUri + "/attachment";
        this.webClient.put()
                .uri(attachmentUri)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(generateBody()))
                .exchange()
                .expectStatus().isNoContent();

        var responseContent = this.webClient.get()
                .uri(attachmentUri)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(ParameterizedTypeReference.forType(byte[].class))
                .getResponseBodyContent();

        assertThat(responseContent).isNotNull();
        assertThat(new String(responseContent)).isEqualTo("test");
    }

    private MultiValueMap<String, HttpEntity<?>> generateBody() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("fileParts",  new ClassPathResource("/foo.txt"));
        return builder.build();
    }

}
