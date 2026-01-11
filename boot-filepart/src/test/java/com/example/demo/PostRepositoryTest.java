package com.example.demo;


import io.r2dbc.spi.Blob;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest()
@Slf4j
@Import(TestcontainersConfiguration.class)
public class PostRepositoryTest {

    @Autowired
    PostRepository posts;

    @Test
    public void testByteBuffer() {
        String s = "testByteBuffer";
        var post = Post.of("r2dbc", "content of r2dbc").withAttachment(ByteBuffer.wrap(s.getBytes()));
        posts.save(post)
                .as(StepVerifier::create)
                .consumeNextWith(saved -> {
                            assertThat(saved.title()).isEqualTo("r2dbc");
                            var attachment = new String(saved.attachment().array());
                            assertThat(attachment).isEqualTo(s);
                        }
                )
                .verifyComplete();
    }

    @Test
    public void testByteArray() {
        String s = "testByteArray";
        var post = Post.of("r2dbc", "content of r2dbc").withCoverImage(s.getBytes());
        posts.save(post)
                .as(StepVerifier::create)
                .consumeNextWith(saved -> {
                            assertThat(saved.title()).isEqualTo("r2dbc");
                            var attachment = new String(saved.coverImage());
                            assertThat(attachment).isEqualTo(s);
                        }
                )
                .verifyComplete();
    }

    @Test
    public void testBlob() {
        String s = "testBlob";
        var post = Post.of("r2dbc", "content of r2dbc").withCoverImageThumbnail(Blob.from(Mono.just(ByteBuffer.wrap(s.getBytes()))));
        posts.save(post)
                .as(StepVerifier::create)
                .consumeNextWith(saved -> {
                            assertThat(saved.title()).isEqualTo("r2dbc");
                            var latch = new CountDownLatch(1);
                            Mono.from(saved.coverImageThumbnail().stream())
                                    .map(it -> new String(it.array()))
                                    .subscribe(it -> {
                                        assertThat(it).isEqualTo(s);
                                        latch.countDown();
                                    });

                            try {
                                latch.await(1000, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                )
                .verifyComplete();
    }

}
