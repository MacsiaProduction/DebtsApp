package ru.m_polukhin.debtsapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJdbcRepositories(basePackages = "ru.m_polukhin.debtsapp.repository")
public class DebtsAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(DebtsAppApplication.class, args);
    }
}
