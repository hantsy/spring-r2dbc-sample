package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
class WebConfig {

    @Bean
    public RouterFunction<ServerResponse> routes(
            BookHandler bookHandler,
            ReviewHandler reviewHandler) {
        return route()
                .path("/books", () -> route()
                        .nest(
                                path(""),
                                () -> route()
                                        .GET("", bookHandler::all)
                                        .POST("", bookHandler::create)
                                        .build()
                        )
                        .nest(
                                path("{id}"),
                                () -> route()
                                        .GET("", bookHandler::get)
                                        .PUT("", bookHandler::update)
                                        .DELETE("", bookHandler::delete)
                                        .nest(
                                                path("comments"),
                                                () -> route()
                                                        .GET("", reviewHandler::getByBookId)
                                                        .POST("", reviewHandler::create)
                                                        .build()
                                        )
                                        .build()
                        )
                        .build()
                ).build();
    }
}
