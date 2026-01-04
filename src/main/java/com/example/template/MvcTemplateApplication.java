package com.example.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MvcTemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(MvcTemplateApplication.class, args);
    }

}
