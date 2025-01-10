package com.example.bootr2dbc;

import com.example.bootr2dbc.common.ContainerConfig;
import org.springframework.boot.SpringApplication;

public class TestR2dbcApplication {

    public static void main(String[] args) {
        SpringApplication.from(R2dbcApplication::main).with(ContainerConfig.class).run(args);
    }
}
