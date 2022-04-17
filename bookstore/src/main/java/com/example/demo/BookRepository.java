package com.example.demo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

interface BookRepository extends R2dbcRepository<Book, UUID> {

    @Query("SELECT * FROM books where title like :title")
    public Flux<Book> findByTitleContains(String title);

    public Flux<BookSummary> findByTitleLike(String title, Pageable pageable);
}
