package com.example.simplylearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SimplyLearnApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimplyLearnApplication.class, args);
    }
}
