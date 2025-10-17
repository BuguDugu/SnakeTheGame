package org.example.snake.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SnakeServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SnakeServerApplication.class, args);
    }
}
