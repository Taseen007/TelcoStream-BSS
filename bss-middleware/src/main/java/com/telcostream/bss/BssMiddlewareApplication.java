package com.telcostream.bss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BssMiddlewareApplication {

    public static void main(String[] args) {
        SpringApplication.run(BssMiddlewareApplication.class, args);
    }
}
