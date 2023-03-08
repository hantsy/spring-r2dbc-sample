package com.example.demo

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class RouterConfiguration {

    @Bean
    fun routes(handler: PostHandler) = coRouter {
        "/posts".nest {
            GET("", handler::all)
            GET("/{id}", handler::get)
            POST("", handler::create)
            PUT("/{id}", handler::update)
            DELETE("/{id}", handler::delete)
        }
    }
}