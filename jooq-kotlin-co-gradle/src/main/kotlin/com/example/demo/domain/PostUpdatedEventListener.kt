package com.example.demo.domain

import com.example.demo.application.EventHandlingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class PostUpdatedEventListener(
    val eventHandlingService: EventHandlingService,
    val applicationScope: CoroutineScope
) {
    companion object {
        private val log = LoggerFactory.getLogger(PostUpdatedEventListener::class.java)
    }

    @EventListener
    fun onPostUpdatedEvent(event: PostUpdatedEvent) {
        log.debug("listen to event:$event")
        applicationScope.launch {
            eventHandlingService.updatePostCommentsCount(event.postId)
        }
    }
}