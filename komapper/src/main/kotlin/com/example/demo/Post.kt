package com.example.demo

import org.komapper.annotation.EnumType.NAME
import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperColumn
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperEnum
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import java.time.LocalDateTime

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