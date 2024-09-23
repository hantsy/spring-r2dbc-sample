# Handling File Upload/Download with Spring WebFlux and Spring Data R2dbc

In [last post](https://hantsy.medium.com/persisting-binary-data-into-postgres-using-spring-data-r2dbc-cefe6afb3e1c), we have explored how to persist binary data into Postgres database using R2dbc/Spring Data R2dbc. In this post, we continue to discuss how to handle file upload and download via Spring WebFlux stack, and of course we will reuse the persistence work in the previous post.

Let's work on the existing project we used in the previous post.

Create a controller `PostController`, then add 3 methods mapped to 3 endpoints.
* `GET posts/?title&page&size` - A method to retrieve all posts.
* `POST posts/{id}` - A method to save new Post.
* `GET posts/{id}` - A method to retrieve Post by id.

```java
@RestController
@RequestMapping("/posts")
@Slf4j
@RequiredArgsConstructor
class PostController {

    private final PostRepository posts;

    @GetMapping
    public Flux<PostSummary> all(@RequestParam(required = true, defaultValue = "") String title,
                                 @RequestParam(required = true, defaultValue = "0") Integer page,
                                 @RequestParam(required = true, defaultValue = "10") Integer size
    ) {
        return posts.findByTitleLike("%" + title + "%", PageRequest.of(page, size));
    }

    @PostMapping
    public Mono<ResponseEntity> create(@RequestBody CreatePostCommand data) {
        return posts.save(Post.builder().title(data.title()).content(data.content()).build())
                .map(saved -> ResponseEntity.created(URI.create("/posts/" + saved.getId())).build());
    }

    @GetMapping("{id}")
    public Mono<ResponseEntity<Post>> get(@PathVariable UUID id) {
        return this.posts.findById(id)
                .map(post -> ResponseEntity.ok().body(post))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

}
```

The `@RequiredArgsConstructor` will append a constructor with the `final` fields as parameters at compile time.

```java
public PostController(PostRepository posts;) {
    this.posts = posts;
}
```

Spring supports injection via constructor for the constructor with parameters if the class has one and only such one constructor.

The `PostSummary` contains the essential info of a `Post`, esp. `title`.

```java
public record PostSummary(UUID id, String title) {
}
```

Add a method into the `PostRepository`.

```java
interface PostRepository extends R2dbcRepository<Post, UUID>{

    public Flux<PostSummary> findByTitleLike(String title, Pageable pageable);
}
```

Notes, in the *reactive* `Repository`, it accepts a `Pageable` as the last parameter in the method parameter list, but it does not return a `Page` object as result. To get the count of the total elements, you should use another count query like the following.

```java
interface PostRepository extends R2dbcRepository<Post, UUID>{
    // ...
    public Long countByTitleLike(String title);
}
```

The `/posts` endpoint get all posts that only includes post titles, binary data in the JSON will be displayed as unrecognized characters.

It is better to use a standalone endpoint to save and read binary data.

Add two endpoints methods to handle attachment upload and download.
* The `PUT posts/{id}/attachment` endpoint reads file *content* from `FilePart` which is similar to Serlvet `Part`, but it is designated for reactive cases. Extract the data into Spring specific `DataBuffer`, and convert to `ByteBuffer` finally.
* The `GET posts/{id}/attachment` endpoint writes the binary data into HTTP response directly. 

```java
@PutMapping("{id}/attachment")
public Mono<ResponseEntity> upload(@PathVariable UUID id,
                                        @RequestPart Mono<FilePart> fileParts) {

    return Mono
            .zip(objects -> {
                        var post = (Post) objects[0];
                        var filePart = (DataBuffer) objects[1];
                        post.setAttachment(filePart.toByteBuffer());
                        return post;
                    },
                    this.posts.findById(id),
                    fileParts.flatMap(filePart -> DataBufferUtils.join(filePart.content()))
            )
            .flatMap(this.posts::save)
            .map(saved -> ResponseEntity.noContent().build());
}

@GetMapping("{id}/attachment")
public Mono<Void> read(@PathVariable UUID id, ServerWebExchange exchange) {
    return this.posts.findById(id)
            .log()
            .map(post -> Mono.just(new DefaultDataBufferFactory().wrap(post.getAttachment())))
            .flatMap(r -> exchange.getResponse().writeWith(r));
}
```    

Create an integration test to verify the file upload and download progress.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class IntegrationTests {

    @LocalServerPort
    private int port;

    private WebTestClient webClient;

    @BeforeEach
    public void setup() {
        this.webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + this.port)
                .codecs(clientCodecConfigurer ->
                        clientCodecConfigurer.defaultCodecs().enableLoggingRequestDetails(true)
                )
                .build();
    }

    @Test
    public void willLoadPosts() {
        this.webClient.get().uri("/posts")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    public void testUploadAndDownload() {
        var locationUri = this.webClient.post()
                .uri("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreatePostCommand("test title", "test content"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .returnResult(ParameterizedTypeReference.forType(Void.class))
                .getResponseHeaders().getLocation();

        log.debug("location uri: {}", locationUri);
        assertThat(locationUri).isNotNull();

        var attachmentUri = locationUri + "/attachment";
        this.webClient.put()
                .uri(attachmentUri)
                .bodyValue(generateBody())
                .exchange()
                .expectStatus().isNoContent();

        var responseContent = this.webClient.get()
                .uri(attachmentUri)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(ParameterizedTypeReference.forType(byte[].class))
                .getResponseBodyContent();

        assertThat(responseContent).isNotNull();
        assertThat(new String(responseContent)).isEqualTo("test");
    }

    private MultiValueMap<String, HttpEntity<?>> generateBody() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("fileParts", new ClassPathResource("/foo.txt", IntegrationTests.class));
        return builder.build();
    }

}
```

In the above codes, `WebEnvironment.RANDOM_PORT` ensure the test application is running at a random port. The `enableLoggingRequestDetails(true)` is to display the HTTP request/response details when sending a request. The `generateBody()` method is used to prepare a test-purpose file multipart form.

The complete [sample codes](https://github.com/hantsy/spring-r2dbc-sample/blob/master/filepart) are shared on my Github account.
