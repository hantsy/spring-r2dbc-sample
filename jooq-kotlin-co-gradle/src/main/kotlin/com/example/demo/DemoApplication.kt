package com.example.demo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication(exclude = [JooqAutoConfiguration::class])
class DemoApplication {


    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Bean
    fun applicationScope() = applicationScope
}

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

