package com.example.demo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.Flow
import org.komapper.annotation.EnumType.NAME
import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperColumn
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperEnum
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.operator.desc
import org.komapper.core.dsl.operator.descNullsLast
import org.komapper.core.dsl.query.first
import org.komapper.core.dsl.query.firstOrNull
import org.komapper.dialect.postgresql.r2dbc.PostgreSqlR2dbcDialect
import org.komapper.r2dbc.DefaultR2dbcDatabaseConfig
import org.komapper.r2dbc.R2dbcDatabase
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.LocalDateTime

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@Configuration(proxyBeanMethods = false)
class KomapperConfiguration {

    @Bean
    fun r2dbcDatabase(connectionFactory: ConnectionFactory) =
        R2dbcDatabase(
            DefaultR2dbcDatabaseConfig(
                connectionFactory = connectionFactory,
                dialect = PostgreSqlR2dbcDialect()
            )
        )
}

@KomapperEntity
@KomapperTable("posts")
data class Post(
    @KomapperId
    @KomapperAutoIncrement
    @KomapperColumn("id")
    val id: Long? = null,

    @KomapperColumn("title")
    val title: String,

    @KomapperColumn("body")
    val body: String,

    @KomapperEnum(type = NAME)
    val status: Status = Status.DRAFT,

    @KomapperCreatedAt
    @KomapperColumn("created_at")
    val createdAt: LocalDateTime? = null,

    @KomapperUpdatedAt
    @KomapperColumn("updated_at")
    val updatedAt: LocalDateTime? = null,
) {
    enum class Status {
        DRAFT, PUBLISHED
    }
}

@Component
class DataInitializer(
    private val r2dbcDatabase: R2dbcDatabase
) {
    companion object {
        private val log = LoggerFactory.getLogger(DataInitializer::class.java)
    }

    @EventListener(ApplicationReadyEvent::class)
    suspend fun init() {
        val p = Meta.post

        r2dbcDatabase.withTransaction {

            // create schema
            r2dbcDatabase.runQuery {
                QueryDsl.drop(p)
                QueryDsl.create(p)
            }

            // insert data
            val newPost = r2dbcDatabase.runQuery {
                QueryDsl.insert(p)
                    .single(
                        Post(
                            title = "Hello, Komapper!",
                            body = "Hello, Komapper!",
                            status = Post.Status.PUBLISHED
                        )
                    )
            }

            // select data
            val foundPost = r2dbcDatabase.runQuery {
                QueryDsl.from(p).where { p.id eq newPost.id }.first()
            }

            log.debug(" found post: {}", foundPost)
        }
    }
}

interface PostRepository {
    fun findAll(): Flow<Post>

    suspend fun findById(id: Long): Post?

    suspend fun save(post: Post): Post

    suspend fun deleteById(id: Long): Long
}

@Repository
class KomapperPostRepository(
    private val r2dbcDatabase: R2dbcDatabase
) : PostRepository {
    val p = Meta.post

    override fun findAll(): Flow<Post> = r2dbcDatabase.flowQuery {
        QueryDsl.from(p).orderBy(p.createdAt.descNullsLast())
    }

    override suspend fun findById(id: Long): Post? = r2dbcDatabase.runQuery {
        QueryDsl.from(p).where { p.id eq id }.firstOrNull()
    }

    override suspend fun save(post: Post): Post = r2dbcDatabase.withTransaction {
        r2dbcDatabase.runQuery {
            QueryDsl.insert(p)
                .single(post)
                .returning()
        }
    }

    override suspend fun deleteById(id: Long): Long = r2dbcDatabase.withTransaction {
        r2dbcDatabase.runQuery {
            QueryDsl.delete(p).where { p.id eq id }
        }
    }

}

@RestController
@RequestMapping("/posts")
class PostController(
    private val postRepository: PostRepository
) {
    @GetMapping("")
    suspend fun findAll(): Flow<Post> = postRepository.findAll()

    @GetMapping("/{id}")
    suspend fun findById(@PathVariable id: Long): ResponseEntity<Post> =
        postRepository.findById(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @PostMapping
    suspend fun save(post: Post): ResponseEntity<Any> =
        postRepository.save(post)
            .let { ResponseEntity.created(URI.create("/posts/" + it.id)).build() }

    @DeleteMapping("/{id}")
    suspend fun deleteById(@PathVariable id: Long): ResponseEntity<Any> =
        postRepository.deleteById(id)
            .let { if (it > 0) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }
}