package com.medicine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedicineSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(MedicineSystemApplication.class, args);
    }
}
