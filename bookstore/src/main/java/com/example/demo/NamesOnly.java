package com.example.demo;

import org.springframework.beans.factory.annotation.Value;

public interface NamesOnly {
    String getFirstName();

    String getLastName();

    default String getName() {
        return getFirstName().concat(" ").concat(getLastName());
    }

    @Value("#{target.firstName + ' ' + target.lastName}")
    String getFullName();

    @Value("#{args[0] + ' ' + target.firstName + '!'}")
    String getSalutation(String prefix);
}
