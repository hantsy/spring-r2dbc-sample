package com.example.demo

import com.example.demo.application.BlogService
import com.example.demo.application.internal.DefaultBlogService
import com.example.demo.domain.JooqConfig
import com.example.demo.domain.R2dbcConfig
import com.example.demo.domain.event.PostUpdatedEvent
import com.example.demo.domain.model.Comment
import com.example.demo.domain.model.Post
import com.example.demo.domain.repository.CommentRepository
import com.example.demo.domain.repository.PostRepository
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@DataR2dbcTest()
class CommentEventDataFlowTest {
    companion object {
        private val log = LoggerFactory.getLogger(CommentEventDataFlowTest::class.java)
    }

    @TestConfiguration
    @Import(TestcontainersConfiguration::class, JooqConfig::class, R2dbcConfig::class)
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
    suspend fun `accept pending comments`() {
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

        eventually(5000.milliseconds) {
            val foundPost = posts.findById(post.id!!)
            foundPost shouldNotBe null

            log.debug("found post: $foundPost")
            foundPost!!.commentsCount!! shouldBeGreaterThan 0
        }

    }

    @Test
    suspend fun `reject pending comments`() {
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

        eventually(5000.milliseconds) {
            val foundPost = posts.findById(post.id!!)
            foundPost shouldNotBe null

            log.debug("found post: $foundPost")
            foundPost!!.commentsCount!! shouldBeGreaterThan 0
        }

    }

    @Test
    suspend fun `reset rejected comments to pending`() {
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

        eventually(5000.milliseconds) {
            val foundPost = posts.findById(post.id!!)
            foundPost shouldNotBe null

            log.debug("found post: $foundPost")
            foundPost!!.commentsCount!! shouldBeGreaterThan 0
        }

        blogService.resetRejectedCommentsOfPost(post.id!!)

        eventually(5000.milliseconds) {
            val foundPost = posts.findById(post.id!!)
            foundPost shouldNotBe null

            log.debug("found post after reset: $foundPost")
            foundPost!!.commentsCount!! shouldBeGreaterThan 0
        }

        blogService.acceptPendingCommentOfPost(post.id!!)

        eventually(5000.milliseconds) {
            val foundPost = posts.findById(post.id!!)
            foundPost shouldNotBe null

            log.debug("found post after accepted: $foundPost")
            foundPost!!.commentsCount!! shouldBeGreaterThan 0
        }

    }

}