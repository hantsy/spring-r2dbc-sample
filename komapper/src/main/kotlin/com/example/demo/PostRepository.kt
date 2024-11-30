package com.example.demo

import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun findAll(): Flow<Post>

    suspend fun findById(id: Long): Post?

    suspend fun save(post: Post): Post

    suspend fun deleteById(id: Long): Long
}