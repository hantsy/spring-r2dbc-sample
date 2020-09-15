package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PgTests {
    @Nested
    @Slf4j
    static class Connections {

        @Test
        public void getFromUrl() {
            var conn = PgConnectionFactories.fromUrl();
            var metadata = conn.getMetadata();
            log.info("ConnectionFactoryMetadata name: {}", metadata.getName());
            assertThat(conn).isNotNull();
        }

        @Test
        public void fromOptions() {
            var conn = PgConnectionFactories.fromOptions();
            assertThat(conn).isNotNull();
        }

        @Test
        public void pgConnectionFactory() {
            var conn = PgConnectionFactories.pgConnectionFactory();
            assertThat(conn).isNotNull();
        }
    }

}
