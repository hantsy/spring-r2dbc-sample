package com.example.demo

import com.example.demo.Post.Status.PUBLISHED
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.first
import org.komapper.r2dbc.R2dbcDatabase
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

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
                            status = PUBLISHED
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