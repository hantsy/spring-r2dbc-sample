package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoRepository todoRepository;

    @GetMapping
    public ResponseEntity getAll(){
        return ResponseEntity.ok(todoRepository.findAll());
    }
}
