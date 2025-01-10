package com.example.bootr2dbc.repositories;

import com.example.bootr2dbc.entities.ReactivePost;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;

public interface ReactivePostRepository
        extends ReactiveCrudRepository<ReactivePost, Long>,
                ReactiveSortingRepository<ReactivePost, Long> {}
