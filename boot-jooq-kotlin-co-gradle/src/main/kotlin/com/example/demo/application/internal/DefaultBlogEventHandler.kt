package com.example.demo.application.internal

import com.example.demo.application.BlogEventHandler
import com.example.demo.domain.event.PostUpdatedEvent
import com.example.demo.domain.model.Comment
import com.example.demo.domain.repository.CommentRepository
import com.example.demo.domain.repository.PostRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
class DefaultBlogEventHandler(
    val postRepository: PostRepository,
    val commentRepository: CommentRepository
) : BlogEventHandler {

    @EventListener(PostUpdatedEvent::class)
    override suspend fun onPostUpdatedEvent(event: PostUpdatedEvent) {
        val id = event.postId
        val count = commentRepository.countByPostIdAndStatus(id, Comment.Status.ACCEPTED)
        postRepository.findById(id)?.apply {
            commentsCount = count
        }?.also { postRepository.save(it) }
    }
}