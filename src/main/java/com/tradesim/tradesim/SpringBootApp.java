package com.tradesim.tradesim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot Application Entry Point.
 * Provides REST API layer for the trading simulator.
 * Run this for web API mode, or TradeSimApp for JavaFX desktop mode.
 */
@SpringBootApplication
@EnableScheduling
public class SpringBootApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }
}