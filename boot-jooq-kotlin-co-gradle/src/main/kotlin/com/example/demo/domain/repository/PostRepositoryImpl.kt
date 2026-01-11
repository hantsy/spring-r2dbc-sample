package com.example.demo.domain.repository

import com.example.demo.domain.model.PostSummary
import com.example.demo.jooq.tables.references.COMMENTS
import com.example.demo.jooq.tables.references.POSTS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class PostRepositoryImpl(private val dslContext: DSLContext) : PostRepositoryCustom {
    override fun findByKeyword(title: String): Flow<PostSummary> {
        val sql = dslContext
            .select(
                POSTS.ID,
                POSTS.TITLE,
                DSL.field("count(comments.id)", SQLDataType.BIGINT)
            )
            .from(
                POSTS
                    .leftJoin(COMMENTS.`as`("comments"))
                    .on(COMMENTS.POST_ID.eq(POSTS.ID))
            )
            .where(
                POSTS.TITLE.like("%$title%")
                    .or(POSTS.CONTENT.like("%$title%"))
                    .or(COMMENTS.CONTENT.like("%$title%"))
            )
            .groupBy(POSTS.ID)


        return Flux.from(sql)
            .map { r -> PostSummary(r.value1(), r.value2(), r.value3()) }
            .asFlow();
    }

    override suspend fun countByKeyword(title: String): Long {
        val sql = dslContext
            .select(
                DSL.field("count(distinct(posts.id))", SQLDataType.BIGINT)
            )
            .from(
                POSTS
                    .leftJoin(COMMENTS.`as`("comments"))
                    .on(COMMENTS.POST_ID.eq(POSTS.ID))
            )
            .where(
                POSTS.TITLE.like("%$title%")
                    .or(POSTS.CONTENT.like("%$title%"))
                    .or(COMMENTS.CONTENT.like("%$title%"))
            )
        return Mono.from(sql).map { it.value1() ?: 0 }.awaitSingle()
    }

}
