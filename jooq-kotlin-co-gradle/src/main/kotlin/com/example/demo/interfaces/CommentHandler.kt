package com.example.demo.interfaces

import com.example.demo.domain.Comment
import com.example.demo.domain.CommentRepository
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import java.net.URI
import java.util.*

@Component
class CommentHandler(val comments: CommentRepository) {
    suspend fun allByPost(req: ServerRequest): ServerResponse {
        val postId = UUID.fromString(req.pathVariable("id"))
        val data = comments.findByPostId(postId)
        return ServerResponse.ok().bodyAndAwait(data)
    }

    suspend fun create(req: ServerRequest): ServerResponse {
        var postId = UUID.fromString(req.pathVariable("id"))
        val data = req.awaitBody(Comment::class)
            .apply {
                postId = postId
            }

        val saved = comments.save(data)
        return ServerResponse.created(URI.create("/comments/${saved.id}")).buildAndAwait()
    }
}