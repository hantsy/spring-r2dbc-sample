package com.example.demo.interfaces

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class RouterConfiguration {

    @Bean
    fun routes(handler: PostHandler, commentsHandler: CommentHandler) = coRouter {
        "/posts".nest {
            GET("", handler::all)
            POST("", handler::create)

            "/{id}".nest {
                GET("", handler::get)
                PUT("", handler::update)
                DELETE("", handler::delete)

                "/comments".nest {
                    GET("", commentsHandler::allByPost)
                    POST("", commentsHandler::create)
                }
            }
        }
    }
}