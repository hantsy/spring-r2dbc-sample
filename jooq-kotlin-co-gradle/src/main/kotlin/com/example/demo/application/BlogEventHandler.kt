package com.example.demo.application

import com.example.demo.domain.event.PostUpdatedEvent

interface BlogEventHandler {
    suspend fun onPostUpdatedEvent(event: PostUpdatedEvent)
}