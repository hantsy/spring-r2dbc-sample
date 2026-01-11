package com.example.demo.domain.repository

import com.example.demo.domain.model.Post
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import java.util.*

interface PostRepository : CoroutineSortingRepository<Post, UUID>,
    CoroutineCrudRepository<Post, UUID>,
    PostRepositoryCustom {
    fun findByStatus(status: Post.Status): Flow<Post>
}