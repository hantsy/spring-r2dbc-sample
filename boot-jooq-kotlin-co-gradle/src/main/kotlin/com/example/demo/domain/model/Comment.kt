package com.example.demo.domain.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

@Table(value = "comments")
data class Comment(
    @Id
    @Column("id")
    val id: UUID? = null,

    @Column("content")
    var content: String? = null,

    @Column("status")
    var status: Status? = Status.PENDING,

    @Column("notes")
    var notes: String? = null,

    @Column("post_id")
    var postId: UUID? = null,

    @Column("created_at")
    @CreatedDate
    val createdAt: LocalDateTime? = null,
){
    enum class Status {
        PENDING, ACCEPTED, REJECTED;
    }

}

