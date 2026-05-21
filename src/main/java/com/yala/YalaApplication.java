package com.yala;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class YalaApplication {

    public static void main(String[] args) {
        SpringApplication.run(YalaApplication.class, args);
    }
}
