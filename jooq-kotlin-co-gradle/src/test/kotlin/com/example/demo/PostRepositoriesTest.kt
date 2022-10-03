package com.example.demo

import com.example.demo.jooq.tables.references.COMMENTS
import com.example.demo.jooq.tables.references.POSTS
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.multiset
import org.jooq.impl.DSL.select
import org.jooq.util.postgres.PostgresDSL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.MountableFile
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


@OptIn(ExperimentalCoroutinesApi::class)
@Testcontainers
@DataR2dbcTest()
@Import(JooqConfig::class, R2dbcConfig::class)
class PostRepositoriesTest {
    companion object {
        private val log = LoggerFactory.getLogger(PostRepositoriesTest::class.java)


        @Container
        val postgreSQLContainer = PostgreSQLContainer("postgres:12")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("/init.sql"),
                "/docker-entrypoint-initdb.d/init.sql"
            )

        @JvmStatic
        @DynamicPropertySource
        fun registerDynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgreSQLContainer.host}:${postgreSQLContainer.firstMappedPort}/${postgreSQLContainer.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgreSQLContainer.username }
            registry.add("spring.r2dbc.password") { postgreSQLContainer.password }
        }

    }

    @Autowired
    lateinit var postRepository: PostRepository

    @Autowired
    lateinit var dslContext: DSLContext

    @BeforeEach
    fun setup() = runTest {
        log.info(" clear sample data ...")
        val deletedPostsCount = Mono.from(dslContext.deleteFrom(POSTS)).awaitSingle()
        log.debug(" deletedPostsCount: $deletedPostsCount")

    }

    @Test
    fun `query sample data`() = runTest {
        log.debug(" add new sample data...")
        val insertPostSql = dslContext.insertInto(POSTS)
            .columns(POSTS.TITLE, POSTS.CONTENT)
            .values("jooq test", "content of Jooq test")
            .returningResult(POSTS.ID)
        val postId = Mono.from(insertPostSql).awaitSingle()
        log.debug(" postId: $postId")

        val insertCommentSql = dslContext.insertInto(COMMENTS)
            .columns(COMMENTS.POST_ID, COMMENTS.CONTENT)
            .values(postId.component1(), "test comments")
            .values(postId.component1(), "test comments 2")

        val insertedCount = Mono.from(insertCommentSql).awaitSingle()

        log.info(" insertedCount: $insertedCount")

        val querySQL = dslContext
            .select(
                POSTS.TITLE,
                POSTS.CONTENT,
                multiset(
                    select(COMMENTS.CONTENT)
                        .from(COMMENTS)
                        .where(COMMENTS.POST_ID.eq(POSTS.ID))
                ).`as`("comments")
            )
            .from(POSTS)
            .orderBy(POSTS.CREATED_AT)

        Flux.from(querySQL).asFlow()
            .onEach { log.info("querySQL result: $it") }
            .collect()

        var posts = postRepository.findByTitleContains("test").toList()
        posts shouldNotBe null
        posts.size shouldBe 1
        posts[0].commentsCount shouldBe 2
    }

    @Test
    fun `test PostgresDSL arrayLength function`() = runTest {
        val list = listOf(
            Post(
                title = "foo", content = "foo post"
            ),
            Post(
                title = "bar", content = "bar post", tags = listOf("Spring", "R2dbc", "Jooq")
            )
        )
        postRepository.saveAll(list).toList().forEach { p ->
            log.info("saved post: $p")
        }

        val result = Flux
            .from(
                dslContext.selectFrom(POSTS)
                    .where(PostgresDSL.arrayLength(POSTS.TAGS).eq(0))
            )
            .asFlow()
            .map {
                Post(
                    id = it.id,
                    title = it.title,
                    content = it.content,
                    tags = it.tags?.map { tag -> tag!! },
                    createdAt = it.createdAt
                )
            }
            .toList()

        log.debug("result is $result")

        val result2 = Flux
            .from(
                dslContext.selectFrom(POSTS)
                    .where(DSL.cardinality(POSTS.TAGS).eq(0))
                    //.where(POSTS.TAGS.isNull)
            )
            .asFlow()
            .map {
                Post(
                    id = it.id,
                    title = it.title,
                    content = it.content,
                    tags = it.tags?.map { tag -> tag!! },
                    createdAt = it.createdAt
                )
            }
            .toList()

        log.debug("result2 is $result2")

        val result3 = Flux
            .from(
                dslContext.select(POSTS.TITLE, POSTS.CONTENT, POSTS.TAGS)
                    .from(POSTS)
                    .where(PostgresDSL.arrayLength(POSTS.TAGS).eq(0))
            )
            .asFlow()
            .map {
                Post(
                    title = it.value1(),
                    content = it.value2(),
                    tags = it.value3()?.map { tag -> tag!! }
                )
            }
            .toList()

        log.debug("result3 is $result3")

        val result4 = Flux
            .from(
                dslContext.select(POSTS.TITLE, POSTS.CONTENT, POSTS.TAGS)
                    .from(POSTS)
                    .where(DSL.cardinality(POSTS.TAGS).eq(0))
            )
            .asFlow()
            .map {
                Post(
                    title = it.value1(),
                    content = it.value2(),
                    tags = it.value3()?.map { tag -> tag!! }
                )
            }
            .toList()

        log.debug("result4 is $result4")
    }

}