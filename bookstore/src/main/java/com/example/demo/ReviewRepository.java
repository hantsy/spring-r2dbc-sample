package com.example.demo;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

interface ReviewRepository extends R2dbcRepository<Review, UUID> {
    Flux<Review> findByBookId(UUID postId);
}
