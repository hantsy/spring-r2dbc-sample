package com.example.demo

import kotlinx.coroutines.flow.Flow

interface PostRepositoryCustom {
    fun findByKeyword(title: String): Flow<PostSummary>
    suspend fun countByKeyword(title: String): Long
}