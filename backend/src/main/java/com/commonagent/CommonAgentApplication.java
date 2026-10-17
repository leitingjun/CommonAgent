package com.commonagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CommonAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommonAgentApplication.class, args);
    }
}
