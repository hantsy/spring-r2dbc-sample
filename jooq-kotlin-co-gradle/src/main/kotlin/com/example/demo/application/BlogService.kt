package com.example.demo.application

import com.example.demo.domain.Comment
import com.example.demo.domain.CommentRepository
import com.example.demo.domain.PostRepository
import com.example.demo.domain.PostUpdatedEvent
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*


@Service
@Transactional
class BlogService(
    val commentRepository: CommentRepository,
    val postRepository: PostRepository,
    val applicationEventPublisher: ApplicationEventPublisher
) {

    companion object {
        private val log = LoggerFactory.getLogger(BlogService::class.java)
    }

    suspend fun acceptCommentOfPost(id: UUID) {
        commentRepository.findByPostId(id)
            .buffer()
            .filter { it.status == Comment.Status.PENDING }
            .onEach {
                it.apply {
                    status = Comment.Status.ACCEPTED
                }
                commentRepository.save(it)
            }
            .onCompletion { log.debug("completed at:" + LocalDateTime.now()) }
            .catch { log.debug("catch error on accepting comments:" + it.message) }
            .collect()

        applicationEventPublisher.publishEvent(PostUpdatedEvent(id))
    }

}