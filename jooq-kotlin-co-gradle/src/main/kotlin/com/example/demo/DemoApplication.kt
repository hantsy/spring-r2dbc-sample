package com.example.demo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import java.net.URI
import java.time.LocalDateTime
import java.util.*


@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

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

@Component
class PostHandler(val posts: PostRepository) {
    suspend fun all(req: ServerRequest?): ServerResponse {
        val data = posts.findAll()
        return ok().bodyAndAwait(data)
    }

    suspend fun create(req: ServerRequest): ServerResponse {
        val data = req.awaitBody(Post::class)
        val saved = posts.save(data)
        return created(URI.create("/posts/${saved.id}")).buildAndAwait()
    }

    suspend fun get(req: ServerRequest): ServerResponse {
        val id = req.pathVariable("id")
        val found = posts.findById(UUID.fromString(id))
        return when {
            found != null -> ok().bodyValueAndAwait(found)
            else -> notFound().buildAndAwait()
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
                return noContent().buildAndAwait()
            }
            else -> notFound().buildAndAwait()
        }
    }

    suspend fun delete(req: ServerRequest): ServerResponse {
        val id = req.pathVariable("id")
        posts.deleteById(UUID.fromString(id))
        return noContent().buildAndAwait()
    }
}

interface PostRepository : CoroutineCrudRepository<Post, UUID> {
    fun findByTitleContains(title: String): Flow<PostSummary>
    fun findByStatus(status: Status): Flow<Post>
}

data class PostSummary(var id: UUID, var title: String)

@Table(value = "posts")
data class Post(
    @Id
    @Column("id")
    val id: UUID? = null,

    @Column("title")
    var title: String? = null,

    @Column("content")
    var content: String? = null,

    @Column("status")
    var status: Status? = Status.DRAFT,

    @Column("created_at")
    @CreatedDate
    val createdAt: LocalDateTime? = null,

    @Column("version")
    @Version
    val version: Long? = null,
)

enum class Status {
    DRAFT, PENDING_MODERATION, PUBLISHED
}