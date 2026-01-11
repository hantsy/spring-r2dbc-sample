package com.example.demo.domain.repository

import com.example.demo.domain.model.PostSummary
import kotlinx.coroutines.flow.Flow

interface PostRepositoryCustom {
    fun findByKeyword(title: String): Flow<PostSummary>
    suspend fun countByKeyword(title: String): Long
}