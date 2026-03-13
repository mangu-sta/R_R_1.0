package com.release.rr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RrApplication {

    public static void main(String[] args) {
        SpringApplication.run(RrApplication.class, args);
    }

}
