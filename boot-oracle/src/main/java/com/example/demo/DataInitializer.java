package com.example.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final TodoRepository todos;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Data initialization is starting...");

        todos.findAll().subscribe(
            data -> log.debug("todo data: {}", data),
            error -> log.error("error: {}", error),
            () -> log.debug("done of data initialization...")
        );
    }
}
