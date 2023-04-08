package com.example.demo.repository

import com.example.demo.web.PostSummary
import com.example.demo.model.Post
import com.example.demo.model.Status
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

interface PostRepository : CoroutineCrudRepository<Post, UUID> {
    fun findByTitleContains(title: String): Flow<PostSummary>
    fun findByStatus(status: Status): Flow<Post>
}