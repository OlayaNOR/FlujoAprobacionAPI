package com.nicolas.flujos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class FlujosApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlujosApplication.class, args);
    }
}
