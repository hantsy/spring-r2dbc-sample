package com.example.demo

import org.springframework.boot.fromApplication

fun main(args: Array<String>) {
    fromApplication<DemoApplication>()
        .with(TestcontainersConfiguration::class.java)
        .run(* args)
}