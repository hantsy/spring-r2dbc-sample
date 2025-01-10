package com.example.bootr2dbc.services;

import com.example.bootr2dbc.entities.ReactiveComments;
import com.example.bootr2dbc.entities.ReactivePost;
import com.example.bootr2dbc.mapper.ReactivePostMapper;
import com.example.bootr2dbc.model.ReactivePostRequest;
import com.example.bootr2dbc.repositories.ReactiveCommentsRepository;
import com.example.bootr2dbc.repositories.ReactivePostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReactivePostService {

    private final ReactivePostRepository reactivePostRepository;
    private final ReactiveCommentsRepository reactiveCommentsRepository;
    private final ReactivePostMapper reactivePostMapper;

    public Flux<ReactivePost> findAllReactivePosts(String sortBy, String sortDir) {
        Sort sort =
                sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                        ? Sort.by(sortBy).ascending()
                        : Sort.by(sortBy).descending();

        return reactivePostRepository.findAll(sort);
    }

    public Mono<ReactivePost> findReactivePostById(Long id) {
        return reactivePostRepository.findById(id);
    }

    public Flux<ReactiveComments> findCommentsForReactivePost(Long id) {
        return reactiveCommentsRepository.findAllByPostId(id);
    }

    @Transactional
    public Mono<ReactivePost> saveReactivePost(ReactivePostRequest reactivePostRequest) {
        ReactivePost reactivePost = reactivePostMapper.mapToReactivePost(reactivePostRequest);
        return reactivePostRepository.save(reactivePost);
    }

    @Transactional
    public Mono<ReactivePost> updateReactivePost(
            ReactivePostRequest reactivePostRequest, ReactivePost reactivePost) {
        this.reactivePostMapper.updateReactivePostFromReactivePostRequest(
                reactivePostRequest, reactivePost);
        return reactivePostRepository.save(reactivePost);
    }

    @Transactional
    public Mono<Void> deleteReactivePostById(Long id) {
        return reactivePostRepository.deleteById(id);
    }

    @Transactional
    public Mono<ResponseEntity<Object>> deleteReactivePostAndCommentsById(Long id) {
        return findReactivePostById(id)
                .flatMap(
                        reactivePost ->
                                reactiveCommentsRepository
                                        .deleteAllByPostId(reactivePost.getId())
                                        .then(
                                                reactivePostRepository.deleteById(
                                                        reactivePost.getId()))
                                        .then(Mono.just(ResponseEntity.noContent().build())))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
}
