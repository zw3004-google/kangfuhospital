package cn.hospital.rehab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KangfuApplication {

    public static void main(String[] args) {
        SpringApplication.run(KangfuApplication.class, args);
    }
}
