package com.meridian.scada;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ScadaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScadaApplication.class, args);
    }
}
