package com.example.demo

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.RouterFunction
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest
//@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PostRouterFunctionTest {
    @Autowired
    private lateinit var routerFunction: RouterFunction<*>

    @MockkBean
    private lateinit var posts: PostRepository

    private lateinit var client: WebTestClient

    @BeforeEach
    fun setup() {
        client = WebTestClient.bindToRouterFunction(routerFunction)
            .configureClient()
            .build()
    }

    @Test
    fun `get all posts`() = runTest {
        coEvery { posts.findAll() } returns flowOf(
            Post(
                id = UUID.randomUUID(),
                title = "test title",
                content = "test content"
            )
        )

        client.get()
            .uri("/posts").accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Post::class.java).hasSize(1)

        coVerify(exactly = 1) { posts.findAll() }
    }

    @Test
    fun `get single post by id`() = runTest {
        coEvery { posts.findById(any<UUID>()) } returns
                Post(
                    id = UUID.randomUUID(),
                    title = "test title",
                    content = "test content"
                )

        val id = UUID.randomUUID()
        client.get()
            .uri("/posts/$id").accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.title").isEqualTo("test title")

        coVerify(exactly = 1) { posts.findById(id) }
    }

    @Test
    fun `get single post by non-existing id`() = runTest {
        coEvery { posts.findById(any<UUID>()) } returns null

        val id = UUID.randomUUID()
        client.get()
            .uri("/posts/$id").accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound

        coVerify(exactly = 1) { posts.findById(id) }
    }

    @Test
    fun `create a post`() = runTest {
        val id = UUID.randomUUID()
        coEvery { posts.save(any<Post>()) } returns
                Post(
                    id = id,
                    title = "update title",
                    content = "update content"
                )

        client.post()
            .uri("/posts").contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Post(title = "update title", content = "update content"))
            .exchange()
            .expectStatus().isCreated
            .expectHeader().location("/posts/$id")


        coVerify(exactly = 1) { posts.save(any<Post>()) }
    }

    @Test
    fun `update a post`() = runTest {
        val id = UUID.randomUUID()
        coEvery { posts.findById(any<UUID>()) } returns
                Post(
                    id = id,
                    title = "test title",
                    content = "test content"
                )
        coEvery { posts.save(any<Post>()) } returns
                Post(
                    id = id,
                    title = "test title",
                    content = "test content"
                )

        client.put()
            .uri("/posts/$id").contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Post(title = "test title", content = "test content"))
            .exchange()
            .expectStatus().isNoContent
        coVerify(exactly = 1) { posts.findById(id) }
        coVerify(exactly = 1) { posts.save(any<Post>()) }
    }

    @Test
    fun `delete a post`() = runTest {
        val id = UUID.randomUUID()
        coEvery { posts.deleteById(any<UUID>()) } returns Unit

        client.delete()
            .uri("/posts/$id")
            .exchange()
            .expectStatus().isNoContent

        coVerify(exactly = 1) { posts.deleteById(id) }
    }

}