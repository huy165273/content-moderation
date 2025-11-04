package com.example.moderation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ModerationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModerationApplication.class, args);
    }
}
