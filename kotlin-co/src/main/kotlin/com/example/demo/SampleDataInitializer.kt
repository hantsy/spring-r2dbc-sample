package com.example.demo

import com.example.demo.model.Post
import com.example.demo.repository.PostRepository
import kotlinx.coroutines.runBlocking
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class SampleDataInitializer(val posts: PostRepository) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        runBlocking {
            posts.save(Post(title = "Sample Post", content = "Sample Content"))
        }
    }

}