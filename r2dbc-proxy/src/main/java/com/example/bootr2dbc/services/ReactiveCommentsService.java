package com.example.bootr2dbc.services;

import com.example.bootr2dbc.entities.ReactiveComments;
import com.example.bootr2dbc.mapper.ReactivePostCommentMapper;
import com.example.bootr2dbc.model.ReactiveCommentRequest;
import com.example.bootr2dbc.repositories.ReactiveCommentsRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReactiveCommentsService {

    private final ReactiveCommentsRepository reactiveCommentsRepository;
    private final ReactivePostCommentMapper reactivePostCommentMapper;

    public Flux<ReactiveComments> findAllReactiveCommentsByPostId(
            Long postId, String sortBy, String sortDir) {
        Sort sort =
                sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                        ? Sort.by(sortBy).ascending()
                        : Sort.by(sortBy).descending();

        return reactiveCommentsRepository.findAllByPostId(postId, sort);
    }

    public Mono<ReactiveComments> findReactiveCommentById(UUID id) {
        return reactiveCommentsRepository.findById(id);
    }

    @Transactional
    public Mono<ReactiveComments> saveReactiveCommentByPostId(
            ReactiveCommentRequest reactiveCommentRequest) {
        ReactiveComments reactiveComments =
                reactivePostCommentMapper.mapToReactivePostComments(reactiveCommentRequest);
        return reactiveCommentsRepository.save(reactiveComments);
    }

    @Transactional
    public Mono<ReactiveComments> updateReactivePostComment(
            ReactiveCommentRequest reactiveCommentRequest, ReactiveComments reactiveComments) {
        reactivePostCommentMapper.updateReactiveCommentRequestFromReactiveComments(
                reactiveCommentRequest, reactiveComments);
        return reactiveCommentsRepository.save(reactiveComments);
    }

    @Transactional
    public Mono<Void> deleteReactiveCommentById(UUID id) {
        return reactiveCommentsRepository.deleteById(id);
    }
}
