package com.example.demo.application.interal

import com.example.demo.application.BlogEventHandlingService
import com.example.demo.domain.model.Comment
import com.example.demo.domain.repository.CommentRepository
import com.example.demo.domain.repository.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
class DefaultBlogEventHandlingService(
    val postRepository: PostRepository,
    val commentRepository: CommentRepository
) : BlogEventHandlingService {

   override suspend fun updatePostCommentsCount(id: UUID) {
        val count = commentRepository.countByPostIdAndStatus(id, Comment.Status.ACCEPTED)
        val post = postRepository.findById(id)?.apply {
            commentsCount = count
        }
        post?.also { postRepository.save(it) }
    }
}