package com.example.bootr2dbc.repositories;

import com.example.bootr2dbc.entities.ReactiveComments;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveCommentsRepository
        extends ReactiveCrudRepository<ReactiveComments, UUID>,
                ReactiveSortingRepository<ReactiveComments, UUID> {

    Flux<ReactiveComments> findAllByPostId(Long postId, Sort sort);

    Flux<ReactiveComments> findAllByPostId(Long postId);

    Mono<Void> deleteAllByPostId(Long id);
}
