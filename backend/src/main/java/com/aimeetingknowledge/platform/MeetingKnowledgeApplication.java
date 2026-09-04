package com.aimeetingknowledge.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MeetingKnowledgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetingKnowledgeApplication.class, args);
    }
}
