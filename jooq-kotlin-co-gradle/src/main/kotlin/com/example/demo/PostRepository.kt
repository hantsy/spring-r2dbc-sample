package com.example.demo

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

interface PostRepository : CoroutineCrudRepository<Post, UUID>, PostRepositoryCustom {
    fun findByStatus(status: Status): Flow<Post>
}