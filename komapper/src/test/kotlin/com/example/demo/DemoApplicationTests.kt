package com.example.demo

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class DemoApplicationTests {

    @Autowired
    private lateinit var postRepository: PostRepository

    @Test
    fun contextLoads() = runTest {
        postRepository.findAll().collect {
            println(it)
        }
    }

}
