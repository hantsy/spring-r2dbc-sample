package com.example.bootr2dbc;

import com.example.bootr2dbc.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class})
public class R2dbcApplication {

    public static void main(String[] args) {
        SpringApplication.run(R2dbcApplication.class, args);
    }
}
