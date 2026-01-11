package com.example.demo

import org.springframework.boot.fromApplication

fun main(args: Array<String>) {
    System.getProperties().putIfAbsent("spring.profiles.active", "dev")
    fromApplication<DemoApplication>()
        .with(TestcontainersConfiguration::class.java)
        .run(* args)
}