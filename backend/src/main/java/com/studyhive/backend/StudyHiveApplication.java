package com.studyhive.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StudyHiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyHiveApplication.class, args);
    }
}
