package com.example.bootr2dbc.services;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;

import com.example.bootr2dbc.entities.ReactivePost;
import com.example.bootr2dbc.mapper.ReactivePostMapper;
import com.example.bootr2dbc.model.ReactivePostRequest;
import com.example.bootr2dbc.repositories.ReactiveCommentsRepository;
import com.example.bootr2dbc.repositories.ReactivePostRepository;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactivePostServiceTest {

    @Mock private ReactivePostRepository reactivePostRepository;

    @Mock private ReactiveCommentsRepository reactiveCommentsRepository;

    @Mock private ReactivePostMapper reactivePostMapper;

    @InjectMocks private ReactivePostService reactivePostService;

    @Test
    void findAllReactivePosts() {
        // given
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        given(reactivePostRepository.findAll(sort)).willReturn(Flux.just(getReactivePost()));

        // when
        Flux<ReactivePost> pagedResult = reactivePostService.findAllReactivePosts("id", "asc");

        // then
        StepVerifier.create(pagedResult)
                .expectNextMatches(
                        reactivePost ->
                                Objects.equals(reactivePost.getId(), getReactivePost().getId())
                                        && reactivePost
                                                .getTitle()
                                                .equals(getReactivePost().getTitle())
                                        && reactivePost
                                                .getContent()
                                                .equals(getReactivePost().getContent()))
                .expectComplete()
                .verify();
    }

    @Test
    void findReactivePostById() {
        // given
        given(reactivePostRepository.findById(1L)).willReturn(Mono.just(getReactivePost()));
        // when
        Mono<ReactivePost> reactivePostMono = reactivePostService.findReactivePostById(1L);
        // then
        StepVerifier.create(reactivePostMono)
                .expectNextMatches(
                        reactivePost ->
                                Objects.equals(reactivePost.getId(), getReactivePost().getId())
                                        && reactivePost
                                                .getTitle()
                                                .equals(getReactivePost().getTitle())
                                        && reactivePost
                                                .getContent()
                                                .equals(getReactivePost().getContent()))
                .expectComplete()
                .verify();
    }

    @Test
    void saveReactivePost() {
        // given
        ReactivePostRequest reactivePostRequest = getReactivePostRequest();
        ReactivePost mappedReactivePost =
                ReactivePost.builder()
                        .content(reactivePostRequest.content())
                        .title(reactivePostRequest.title())
                        .build();
        given(reactivePostMapper.mapToReactivePost(reactivePostRequest))
                .willReturn(mappedReactivePost);
        given(reactivePostRepository.save(mappedReactivePost))
                .willReturn(Mono.just(getReactivePost()));
        // when
        Mono<ReactivePost> persistedReactivePost =
                reactivePostService.saveReactivePost(reactivePostRequest);
        // then
        StepVerifier.create(persistedReactivePost)
                .expectNextMatches(
                        reactivePost ->
                                reactivePost.getTitle().equals(getReactivePost().getTitle())
                                        && reactivePost
                                                .getContent()
                                                .equals(getReactivePost().getContent()))
                .expectComplete()
                .verify();
    }

    @Test
    void deleteReactivePostById() {
        // given
        given(reactivePostRepository.deleteById(1L)).willReturn(Mono.empty());
        // when
        Mono<Void> voidMono = reactivePostService.deleteReactivePostById(1L);
        // then
        StepVerifier.create(voidMono).expectComplete().verify();
        verify(reactivePostRepository, times(1)).deleteById(1L);
    }

    private ReactivePost getReactivePost() {
        ReactivePost reactivePost = new ReactivePost();
        reactivePost.setId(1L);
        reactivePost.setTitle("junitTitle");
        reactivePost.setContent("junitContent");
        return reactivePost;
    }

    private ReactivePostRequest getReactivePostRequest() {
        return new ReactivePostRequest("junitTitle", "junitContent");
    }
}
