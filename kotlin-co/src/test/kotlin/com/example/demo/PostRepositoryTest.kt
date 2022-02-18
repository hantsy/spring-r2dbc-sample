package com.example.demo

import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.MountableFile

@OptIn(ExperimentalCoroutinesApi::class)
@DataR2dbcTest
@Testcontainers
class PostRepositoryTest {
    companion object {
        private val log = LoggerFactory.getLogger(PostRepositoryTest::class.java)

        @Container
        private val postgreSQLContainer: PostgreSQLContainer<*> = PostgreSQLContainer<Nothing>("postgres:12")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("init.sql"),
                "/docker-entrypoint-initdb.d/init.sql"
            )

        @DynamicPropertySource
        @JvmStatic
        fun registerDynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                ("r2dbc:postgresql://"
                        + postgreSQLContainer.host + ":" + postgreSQLContainer.firstMappedPort
                        + "/" + postgreSQLContainer.databaseName)
            }
            registry.add("spring.r2dbc.username") { postgreSQLContainer.username }
            registry.add("spring.r2dbc.password") { postgreSQLContainer.password }
        }
    }

    @Autowired
    lateinit var dbclient: DatabaseClient

    @Autowired
    lateinit var template: R2dbcEntityTemplate

    @Autowired
    lateinit var posts: PostRepository

    @BeforeEach
    fun setup() = runTest {
        val deleted = template.delete(Post::class.java).all().awaitSingle()
        log.debug("clean todo list before tests: $deleted")
    }

    @Test
    fun testDatabaseClientExisted() {
        assertNotNull(dbclient)
    }

    @Test
    fun testR2dbcEntityTemplateExisted() {
        assertNotNull(template)
    }

    @Test
    fun testPostRepositoryExisted() {
        assertNotNull(posts)
    }

    @Test
    fun testInsertAndQuery() = runTest {
        val data = Post(title = "test title", content = "test content")
        val saved = posts.save(data)
        // verify id is inserted.
        assertNotNull(saved.id)

        val found = posts.findById(saved.id!!)!!
        //verify the saved data
        assertThat(found.title).isEqualTo("test title")
        assertThat(found.status).isEqualTo(Status.DRAFT)

        found.apply {
            title = "update title"
            status = Status.PENDING_MODERATION
        }
        posts.save(found)
        val updatedPosts = posts.findByTitleContains("update")

        //verify the updated title
        assertThat(updatedPosts.count()).isEqualTo(1)
        assertThat(updatedPosts.toList()[0].title).isEqualTo("update title")
    }

    @Test//using kotest assertions
    fun testPublishedPosts() = runTest {
        val data = Post(title = "test title", content = "test content", status = Status.PUBLISHED)
        val saved = posts.save(data)

        // verify id is inserted.
        saved.id shouldNotBe null

        val publishedPosts = posts.findByStatus(Status.PUBLISHED)

        // find by status PUBLISHED should contain results
        publishedPosts.count() shouldBeEqualComparingTo 1
        publishedPosts.toList()[0].status shouldBe Status.PUBLISHED
    }
}
