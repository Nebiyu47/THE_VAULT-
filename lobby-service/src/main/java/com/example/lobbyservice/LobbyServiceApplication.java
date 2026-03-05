package com.example.lobbyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoRepositories
@EnableScheduling
public class LobbyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LobbyServiceApplication.class, args);
    }

}
