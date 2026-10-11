package com.coldchain.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ColdChainWebApplication {
    public static void main(String[] args) { SpringApplication.run(ColdChainWebApplication.class, args); }
}
