package com.example.demo

import kotlinx.coroutines.flow.Flow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.*


@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
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

    @Column("version")
    @Version
    val version: Long? = null,
) {
    enum class Status {
        DRAFT, PENDING_MODERATION, PUBLISHED
    }
}

interface PostRepository : CoroutineCrudRepository<Post, UUID> {
    fun findByTitleContains(title: String): Flow<PostSummary>
    fun findByStatus(status: Post.Status): Flow<Post>
}