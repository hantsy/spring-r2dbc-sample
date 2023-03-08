package com.example.demo

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import java.net.URI
import java.util.*

@Component
class PostHandler(val posts: PostRepository) {
    suspend fun all(req: ServerRequest): ServerResponse {
        //val data = posts.findAll()
        val title = req.queryParamOrNull("title") ?: ""
        val data = posts.findByKeyword(title)
        return ServerResponse.ok().bodyAndAwait(data)
    }

    suspend fun create(req: ServerRequest): ServerResponse {
        val data = req.awaitBody(Post::class)
        val saved = posts.save(data)
        return ServerResponse.created(URI.create("/posts/${saved.id}")).buildAndAwait()
    }

    suspend fun get(req: ServerRequest): ServerResponse {
        val id = req.pathVariable("id")
        val found = posts.findById(UUID.fromString(id))
        return when {
            found != null -> ServerResponse.ok().bodyValueAndAwait(found)
            else -> ServerResponse.notFound().buildAndAwait()
        }

    }

    suspend fun update(req: ServerRequest): ServerResponse {
        val id = req.pathVariable("id")
        val data = req.awaitBody(Post::class)
        val found = posts.findById(UUID.fromString(id))
        return when {
            found != null -> {
                found.apply {
                    title = data.title
                    content = data.content
                }
                posts.save(found)
                return ServerResponse.noContent().buildAndAwait()
            }

            else -> ServerResponse.notFound().buildAndAwait()
        }
    }

    suspend fun delete(req: ServerRequest): ServerResponse {
        val id = req.pathVariable("id")
        posts.deleteById(UUID.fromString(id))
        return ServerResponse.noContent().buildAndAwait()
    }
}