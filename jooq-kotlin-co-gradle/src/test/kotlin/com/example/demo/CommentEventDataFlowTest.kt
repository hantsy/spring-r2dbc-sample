package com.example.demo

import com.example.demo.application.BlogService
import com.example.demo.application.interal.DefaultBlogService
import com.example.demo.domain.JooqConfig
import com.example.demo.domain.R2dbcConfig
import com.example.demo.domain.event.PostUpdatedEvent
import com.example.demo.domain.model.Comment
import com.example.demo.domain.model.Post
import com.example.demo.domain.repository.CommentRepository
import com.example.demo.domain.repository.PostRepository
import io.kotest.framework.concurrency.eventually
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.MountableFile

@OptIn(ExperimentalCoroutinesApi::class)
@Testcontainers
@DataR2dbcTest()
@Import(JooqConfig::class, R2dbcConfig::class)
class CommentEventDataFlowTest {
    companion object {
        private val log = LoggerFactory.getLogger(CommentEventDataFlowTest::class.java)


        @Container
        val postgreSQLContainer = PostgreSQLContainer("postgres:12")
            .withCopyToContainer(
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

    @TestConfiguration
    @ComponentScan(basePackageClasses = [DefaultBlogService::class, PostUpdatedEvent::class])
    class TestConfig {}

    @Autowired
    lateinit var template: R2dbcEntityTemplate

    @Autowired
    lateinit var posts: PostRepository

    @Autowired
    lateinit var comments: CommentRepository

    @Autowired
    lateinit var blogService: BlogService

    @BeforeEach
    fun setup() = runTest {
        val deleted = template.delete(Post::class.java).all().awaitSingle()
        log.debug("deleted posts: $deleted")
    }

    @Test
    fun `accept pending comments`() = runTest(StandardTestDispatcher(), 300_000) {
        val post = template.insert(Post(title = "Post #", content = "Content of post")).awaitSingle()
        val data = (1..30)
            .map {
                Comment(
                    postId = post.id,
                    status = if (it % 3 == 0) Comment.Status.ACCEPTED else Comment.Status.PENDING,
                    content = "Comment of post $it"
                )
            }

        comments.saveAll(data)
            .collect { log.debug("saved coment: $it") }

        val count = comments.countByStatus(Comment.Status.ACCEPTED)
        log.debug("accepted comment count : $count")
        comments.findByStatus(Comment.Status.ACCEPTED)
            .onEach { log.debug("accepted comment: $it") }
            .collect()

        blogService.acceptPendingCommentOfPost(post.id!!)

        eventually(5000) {
            val foundPost = posts.findById(post.id!!)
            foundPost shouldNotBe null

            log.debug("found post: $foundPost")
            foundPost!!.commentsCount!! shouldBeGreaterThan 0
        }

    }

    @Test
    fun `reject pending comments`() = runTest {
        val post = template.insert(Post(title = "Post #", content = "Content of post")).awaitSingle()
        val data = (1..30)
            .map {
                Comment(
                    postId = post.id,
                    status = if (it % 3 == 0) Comment.Status.ACCEPTED else Comment.Status.PENDING,
                    content = "Comment of post $it"
                )
            }

        comments.saveAll(data)
            .collect { log.debug("saved comment: $it") }

        val count = comments.countByStatus(Comment.Status.ACCEPTED)
        log.debug("accepted comment count : $count")
        comments.findByStatus(Comment.Status.ACCEPTED)
            .onEach { log.debug("accepted comment: $it") }
            .collect()

        blogService.rejectPendingCommentOfPost(post.id!!)

        eventually(5000) {
            val foundPost = posts.findById(post.id!!)
            foundPost shouldNotBe null

            log.debug("found post: $foundPost")
            foundPost!!.commentsCount!! shouldBeGreaterThan 0
        }

    }

    @Test
    fun `reset rejected comments to pending`() = runTest {
        val post = template.insert(Post(title = "Post #", content = "Content of post")).awaitSingle()
        val data = (1..30)
            .map {
                Comment(
                    postId = post.id,
                    status = if (it % 3 == 0) Comment.Status.ACCEPTED else Comment.Status.PENDING,
                    content = "Comment of post $it"
                )
            }

        comments.saveAll(data)
            .collect { log.debug("saved comment: $it") }

        val count = comments.countByStatus(Comment.Status.ACCEPTED)
        log.debug("accepted comment count : $count")
        comments.findByStatus(Comment.Status.ACCEPTED)
            .onEach { log.debug("accepted comment: $it") }
            .collect()

        blogService.rejectPendingCommentOfPost(post.id!!)

        eventually(5000) {
            val foundPost = posts.findById(post.id!!)
            foundPost shouldNotBe null

            log.debug("found post: $foundPost")
            foundPost!!.commentsCount!! shouldBeGreaterThan 0
        }

        blogService.resetRejectedCommentsOfPost(post.id!!)

        eventually(5000) {
            val foundPost = posts.findById(post.id!!)
            foundPost shouldNotBe null

            log.debug("found post after reset: $foundPost")
            foundPost!!.commentsCount!! shouldBeGreaterThan 0
        }

        blogService.acceptPendingCommentOfPost(post.id!!)

        eventually(5000) {
            val foundPost = posts.findById(post.id!!)
            foundPost shouldNotBe null

            log.debug("found post after accepted: $foundPost")
            foundPost!!.commentsCount!! shouldBeGreaterThan 0
        }

    }

}