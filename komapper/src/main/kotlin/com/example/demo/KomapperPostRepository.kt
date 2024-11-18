package com.example.demo

import kotlinx.coroutines.flow.Flow
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.operator.descNullsLast
import org.komapper.core.dsl.query.firstOrNull
import org.komapper.r2dbc.R2dbcDatabase
import org.springframework.stereotype.Repository

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