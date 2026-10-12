package com.menumind.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MenuMind AI - Main Application Entry Point
 * 
 * This class boots the Spring Boot framework, initializes the Spring Application Context,
 * and scans all components under 'com.menumind.backend'.
 */
@SpringBootApplication
public class MenuMindApplication {

    static {
        // Prevent JVM from caching negative DNS responses during transient network latency
        java.security.Security.setProperty("networkaddress.cache.negative.ttl", "0");
    }

    public static void main(String[] args) {
        SpringApplication.run(MenuMindApplication.class, args);
    }
}
