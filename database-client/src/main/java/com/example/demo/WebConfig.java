package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@EnableWebFlux
public class WebConfig {

    @Bean
    public RouterFunction<ServerResponse> routes(PostHandler postController) {
        return route()
                .GET("/posts", postController::all)
                .POST("/posts", postController::create)
                .GET("/posts/{id}", postController::get)
                .PUT("/posts/{id}", postController::update)
                .DELETE("/posts/{id}", postController::delete)
                .build();
    }
}
