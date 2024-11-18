package com.example.demo

import kotlinx.coroutines.flow.Flow
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/posts")
class PostController(
    private val postRepository: PostRepository
) {
    @GetMapping("")
    suspend fun findAll(): Flow<Post> = postRepository.findAll()

    @GetMapping("/{id}")
    suspend fun findById(@PathVariable id: Long): ResponseEntity<Post> =
        postRepository.findById(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @PostMapping
    suspend fun save(post: Post): ResponseEntity<Any> =
        postRepository.save(post)
            .let { ResponseEntity.created(URI.create("/posts/" + it.id)).build() }

    @DeleteMapping("/{id}")
    suspend fun deleteById(@PathVariable id: Long): ResponseEntity<Any> =
        postRepository.deleteById(id)
            .let { if (it > 0) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }
}