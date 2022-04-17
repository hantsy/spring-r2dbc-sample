package com.example.demo;

import org.springframework.data.relational.core.mapping.Column;

public record Address(
        String street,
        @Column("zipcode")
        String zipCode,
        @Column("citys")
        String city
) {
}
