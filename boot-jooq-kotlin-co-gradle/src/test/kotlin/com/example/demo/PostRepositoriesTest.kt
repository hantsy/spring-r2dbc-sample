package com.example.demo

import com.example.demo.domain.JooqConfig
import com.example.demo.domain.R2dbcConfig
import com.example.demo.domain.model.Post
import com.example.demo.domain.repository.PostRepository
import com.example.demo.jooq.tables.references.COMMENTS
import com.example.demo.jooq.tables.references.POSTS
import io.kotest.inspectors.forAny
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
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.*
import org.jooq.impl.SQLDataType
import org.jooq.util.postgres.PostgresDSL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtensionConfig
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate


@OptIn(ExperimentalCoroutinesApi::class)
@DataR2dbcTest
// @Import(TestcontainersConfiguration::class, JooqConfig::class, R2dbcConfig::class)
// see: https://stackoverflow.com/questions/79860529/spring-boot-4-and-java-25
// @SpringExtensionConfig(useTestClassScopedExtensionContext = true)
class PostRepositoriesTest {
    companion object {
        private val log = LoggerFactory.getLogger(PostRepositoriesTest::class.java)
    }

    @TestConfiguration
    @Import(TestcontainersConfiguration::class, JooqConfig::class, R2dbcConfig::class)
    class TestConfig {

    }

    @Autowired
    lateinit var postRepository: PostRepository

    @Autowired
    lateinit var dslContext: DSLContext

    @BeforeEach
    fun setup() = runTest {
        log.info(" clear sample data ...")
        val deletedPostsCount = Mono.from(dslContext.deleteFrom(POSTS)).awaitSingle()
        // postRepository.deleteAll()
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

        val posts = postRepository.findByKeyword("test").toList()
        posts shouldNotBe null
        posts.size shouldBe 1
        posts[0].commentsCount shouldBe 2

        postRepository.countByKeyword("test") shouldBe 1
    }

    @Test
    //see: https://github.com/jOOQ/jOOQ/issues/14047o
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


    @Nested
    inner class CountPerDayTests {

        @Test
        fun `test posts count per day statistics`() = runTest {
            val list = listOf(
                Post(
                    title = "foo",
                    content = "foo post"
                ),
                Post(
                    title = "bar",
                    content = "bar post",
                    tags = listOf("Spring", "R2dbc", "Jooq")
                )
            )
            postRepository.saveAll(list).toList().forEach { p ->
                log.info("saved post: $p")
            }

            val startDate = LocalDate.now().minusDays(7)
            val endDate = LocalDate.now()

            val daysTable: org.jooq.Table<*> = DSL.table("generate_series({0}, {1}, '1 day')", startDate, endDate)
                .`as`("days", "d")
            val dayField = DSL.field(DSL.name("days", "d"), SQLDataType.LOCALDATE)

            if (log.isDebugEnabled) {
                log.debug("generated days from generate_series, start: {}, end: {}", startDate, endDate)
                Flux.from(dslContext.selectFrom(daysTable))
                    .asFlow()
                    .toList()
                    .forEach { log.debug("day: {}", it.getValue(0)) }
            }

            var where: Condition = DSL.trueCondition()

            val sql = dslContext.select(field("count(posts.id)", SQLDataType.BIGINT), dayField)
                .from(
                    daysTable
                        .leftJoin(
                            POSTS
                        )
                        .on(
                            year(POSTS.CREATED_AT).eq(year(dayField))
                                .and(
                                    month(POSTS.CREATED_AT).eq(month(dayField))
                                        .and(day(POSTS.CREATED_AT).eq(day(dayField)))
                                )
                        )
                )
                .where(where)
                .groupBy(dayField)
                .orderBy(dayField)

            val result = Flux
                .from(sql)
                .asFlow()
                .map {
                    PostCountPerDay(
                        count = it.value1() ?: 0L,
                        date = it.value2()
                    )
                }
                .toList()

            log.debug("calculated result: {}", result)

            result.size shouldBe 8 //(including enddate)
            result.last().count shouldBe 2
            result.subList(0, 8).forAny { it.count shouldBe 0 }
        }
    }
}

data class PostCountPerDay(val count: Long, val date: LocalDate)
