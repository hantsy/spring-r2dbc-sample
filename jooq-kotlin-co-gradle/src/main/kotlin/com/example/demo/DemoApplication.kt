package com.example.demo

import com.example.demo.jooq.tables.references.COMMENTS
import com.example.demo.jooq.tables.references.POSTS
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.using
import org.jooq.impl.SQLDataType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Flux
import java.net.URI
import java.time.LocalDateTime
import java.util.*


@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@Configuration
@EnableR2dbcAuditing
class R2dbcConfig {}

@Configuration
class JooqConfig {

    @Bean
    fun dslContext(connectionFactory: ConnectionFactory) =
        using(TransactionAwareConnectionFactoryProxy(connectionFactory), SQLDialect.POSTGRES)

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
    suspend fun all(req: ServerRequest): ServerResponse {
        //val data = posts.findAll()
        val title = req.queryParamOrNull("title") ?: ""
        val data = posts.findByTitleContains(title)
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

interface PostRepository : CoroutineCrudRepository<Post, UUID>, CustomPostRepository {
    fun findByStatus(status: Status): Flow<Post>
}

interface CommentRepository : CoroutineSortingRepository<Comment, UUID>

interface CustomPostRepository {
    fun findByTitleContains(title: String): Flow<PostSummary>
}

class PostRepositoryImpl(val dslContext: DSLContext) : CustomPostRepository {
    override fun findByTitleContains(title: String): Flow<PostSummary> {
        val sql = dslContext
            .select(
                POSTS.ID,
                POSTS.TITLE,
                field("count(comments.id)", SQLDataType.BIGINT)
            )
            .from(
                POSTS
                    .leftJoin(COMMENTS.`as`("comments"))
                    .on(COMMENTS.POST_ID.eq(POSTS.ID))
            )
            .where(POSTS.TITLE.like("%$title%"))
            .groupBy(POSTS.ID)


        return Flux.from(sql)
            .map { r -> PostSummary(r.value1(), r.value2(), r.value3()) }
            .asFlow();
    }

}

data class PostSummary(var id: UUID? = null, var title: String? = null, val commentsCount: Long? = 0)

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

@Table(value = "comments")
data class Comment(
    @Id
    @Column("id")
    val id: UUID? = null,

    @Column("content")
    var content: String? = null,

    @Column("created_at")
    @CreatedDate
    val createdAt: LocalDateTime? = null,
)