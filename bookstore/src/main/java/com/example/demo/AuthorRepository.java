package com.example.demo;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface AuthorRepository extends R2dbcRepository<Author, UUID> {
}
