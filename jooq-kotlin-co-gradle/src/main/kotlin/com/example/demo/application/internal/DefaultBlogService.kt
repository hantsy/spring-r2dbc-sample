package com.example.demo.application.internal

import com.example.demo.application.BlogService
import com.example.demo.domain.event.PostUpdatedEvent
import com.example.demo.domain.model.Comment
import com.example.demo.domain.repository.CommentRepository
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.transactional
import java.time.LocalDateTime
import java.util.*


@Service
@Transactional
class DefaultBlogService(
    val commentRepository: CommentRepository,
    val applicationEventPublisher: ApplicationEventPublisher,
    val transactionalOperator: TransactionalOperator
) : BlogService {

    companion object {
        private val log = LoggerFactory.getLogger(DefaultBlogService::class.java)
    }

    override suspend fun acceptPendingCommentOfPost(id: UUID) {
        commentRepository.findByPostId(id)
            .buffer()
            .filter { it.status == Comment.Status.PENDING }
            .transactional(transactionalOperator)
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

    override suspend fun rejectPendingCommentOfPost(id: UUID) {
        commentRepository.findByPostId(id)
            .buffer()
            .filter { it.status == Comment.Status.PENDING }
            .transactional(transactionalOperator)
            .onEach {
                it.apply {
                    status = Comment.Status.REJECTED
                }
                commentRepository.save(it)
            }
            .onCompletion { log.debug("completed at:" + LocalDateTime.now()) }
            .catch { log.debug("catch error on rejecting comments:" + it.message) }
            .collect()

        applicationEventPublisher.publishEvent(PostUpdatedEvent(id))
    }

    override suspend fun resetRejectedCommentsOfPost(id: UUID) {
        commentRepository.findByPostId(id)
            .buffer()
            .filter { it.status == Comment.Status.PENDING }
            .transactional(transactionalOperator)
            .onEach {
                it.apply {
                    status = Comment.Status.PENDING
                    notes = "please update it"
                }
                commentRepository.save(it)
            }
            .onCompletion { log.debug("completed at:" + LocalDateTime.now()) }
            .catch { log.debug("catch error on rejecting comments:" + it.message) }
            .collect()

        applicationEventPublisher.publishEvent(PostUpdatedEvent(id))
    }

}