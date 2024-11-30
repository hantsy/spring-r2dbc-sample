package com.example.demo

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class DemoApplicationTests {
    companion object {
        private val log = LoggerFactory.getLogger(DemoApplicationTests::class.java)
    }

    @Autowired
    private lateinit var postRepository: PostRepository

    @BeforeEach
    fun setup(): Unit = runBlocking {
        val deleted = postRepository.deleteAll()

        log.debug("deleted posts: $deleted")

        val data = listOf("An introduction to R2dbc", "Komapper and Spring R2dbc")
            .map {
                Post(title = it, body = "content of $it")
            }
        postRepository.saveAll(data).forEach { p -> log.debug("saved post: $p") }
    }

    @Test
    fun contextLoads() = runTest {
        postRepository.findAll().collect {
            log.debug("found post: $it")
        }
    }

}
