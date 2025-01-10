package com.example.bootr2dbc;

import com.example.bootr2dbc.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class ApplicationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        this.webTestClient.get().uri("/api/posts/").exchange().expectStatus().isUnauthorized();
    }
}
