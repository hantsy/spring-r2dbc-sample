package com.example.demo.application

import java.util.*

interface BlogService {
    suspend fun acceptPendingCommentOfPost(id: UUID)

    suspend fun rejectPendingCommentOfPost(id: UUID)

    suspend fun resetRejectedCommentsOfPost(id: UUID)
}