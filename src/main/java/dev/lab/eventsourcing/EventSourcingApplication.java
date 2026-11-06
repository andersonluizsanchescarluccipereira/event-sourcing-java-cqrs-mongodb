package dev.lab.eventsourcing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EventSourcingApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventSourcingApplication.class, args);
    }
}
