package com.example.demo.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

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

    @Column("updated_at")
    @LastModifiedDate
    val updatedAt: LocalDateTime? = null,

    @Column("version")
    @Version
    val version: Long? = null,
)