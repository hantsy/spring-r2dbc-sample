package com.example.demo.application

import com.example.demo.domain.Comment
import com.example.demo.domain.CommentRepository
import com.example.demo.domain.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
class EventHandlingService(
    val postRepository: PostRepository,
    val commentRepository: CommentRepository
) {

    suspend fun updatePostCommentsCount(id: UUID) {
        val count = commentRepository.countByPostIdAndStatus(id, Comment.Status.ACCEPTED)
        val post = postRepository.findById(id)?.apply {
            commentsCount = count
        }
        post?.also { postRepository.save(it) }
    }
}