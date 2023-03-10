package com.example.demo.domain.repository

import com.example.demo.domain.model.Comment
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import java.util.*

interface CommentRepository : CoroutineSortingRepository<Comment, UUID>,
    CoroutineCrudRepository<Comment, UUID> {

    fun findByStatus(status: Comment.Status): Flow<Comment>
    fun findByPostId(postId: UUID): Flow<Comment>
    suspend fun countByPostIdAndStatus(postId: UUID, status: Comment.Status): Long
}