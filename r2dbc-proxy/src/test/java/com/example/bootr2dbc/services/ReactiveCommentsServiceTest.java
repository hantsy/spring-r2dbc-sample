package com.example.bootr2dbc.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;

import com.example.bootr2dbc.entities.ReactiveComments;
import com.example.bootr2dbc.mapper.ReactivePostCommentMapper;
import com.example.bootr2dbc.model.ReactiveCommentRequest;
import com.example.bootr2dbc.repositories.ReactiveCommentsRepository;
import java.util.UUID;
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
class ReactiveCommentsServiceTest {

    @Mock private ReactiveCommentsRepository reactiveCommentsRepository;

    @Mock private ReactivePostCommentMapper reactivePostCommentMapper;

    @InjectMocks private ReactiveCommentsService reactiveCommentsService;

    @Test
    void findAllReactiveComments() {
        // given
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        given(reactiveCommentsRepository.findAllByPostId(1L, sort))
                .willReturn(Flux.just(getReactiveComments()));

        // when
        Flux<ReactiveComments> pagedResult =
                reactiveCommentsService.findAllReactiveCommentsByPostId(1L, "id", "asc");

        // then
        assertThat(pagedResult).isNotNull();
        StepVerifier.create(pagedResult)
                .expectNextMatches(
                        reactivePostComment ->
                                reactivePostComment
                                                .getPostId()
                                                .equals(getReactiveComments().getPostId())
                                        && reactivePostComment
                                                .getContent()
                                                .equals(getReactiveComments().getContent()))
                .expectComplete()
                .verify();
    }

    @Test
    void findReactiveCommentsById() {
        // given
        UUID postCommentId = UUID.randomUUID();
        given(reactiveCommentsRepository.findById(postCommentId))
                .willReturn(Mono.just(getReactiveComments()));
        // when
        Mono<ReactiveComments> reactiveCommentsMono =
                reactiveCommentsService.findReactiveCommentById(postCommentId);
        // then
        StepVerifier.create(reactiveCommentsMono)
                .expectNextMatches(
                        reactivePostComment ->
                                reactivePostComment
                                                .getPostId()
                                                .equals(getReactiveComments().getPostId())
                                        && reactivePostComment
                                                .getContent()
                                                .equals(getReactiveComments().getContent()))
                .expectComplete()
                .verify();
    }

    @Test
    void saveReactiveComments() {
        // given
        ReactiveCommentRequest reactivePostCommentsRequest = getReactiveCommentRequest();
        ReactiveComments mappedReactivePostComments =
                ReactiveComments.builder()
                        .content(reactivePostCommentsRequest.content())
                        .title(reactivePostCommentsRequest.title())
                        .postId(reactivePostCommentsRequest.postId())
                        .build();
        given(reactivePostCommentMapper.mapToReactivePostComments(reactivePostCommentsRequest))
                .willReturn(mappedReactivePostComments);
        given(reactiveCommentsRepository.save(mappedReactivePostComments))
                .willReturn(Mono.just(getReactiveComments()));
        // when
        Mono<ReactiveComments> persistedReactiveComments =
                reactiveCommentsService.saveReactiveCommentByPostId(reactivePostCommentsRequest);
        // then
        StepVerifier.create(persistedReactiveComments)
                .expectNextMatches(
                        reactivePostComment ->
                                reactivePostComment
                                                .getPostId()
                                                .equals(getReactiveComments().getPostId())
                                        && reactivePostComment
                                                .getContent()
                                                .equals(getReactiveComments().getContent()))
                .expectComplete()
                .verify();
    }

    @Test
    void deleteReactiveCommentsById() {
        // given
        UUID postCommentId = UUID.randomUUID();
        given(reactiveCommentsRepository.deleteById(postCommentId)).willReturn(Mono.empty());
        // when
        reactiveCommentsService.deleteReactiveCommentById(postCommentId);
        // then
        verify(reactiveCommentsRepository, times(1)).deleteById(postCommentId);
    }

    private ReactiveComments getReactiveComments() {
        ReactiveComments reactiveComments = new ReactiveComments();
        reactiveComments.setId(UUID.randomUUID());
        reactiveComments.setContent("junitContent");
        reactiveComments.setPostId(1L);
        return reactiveComments;
    }

    private ReactiveCommentRequest getReactiveCommentRequest() {
        return new ReactiveCommentRequest("junitTitle", "junitContent", 1L, true);
    }
}
