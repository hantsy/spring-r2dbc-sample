package com.example.demo.application

import java.util.*

interface BlogEventHandlingService {
    suspend fun updatePostCommentsCount(id: UUID)
}