package com.tradesim.tradesim.controller;

import com.tradesim.tradesim.db.Database;
import com.tradesim.tradesim.model.Stock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for Stock Market Data.
 * Exposes market data via HTTP API - demonstrates Spring Boot integration.
 */
@RestController
@RequestMapping("/api/stocks")
@CrossOrigin(origins = "*")
public class StockRestController {

    private final Database database;

    @Autowired
    public StockRestController(Database database) {
        this.database = database;
    }

    @GetMapping
    public List<Stock> getAllStocks() {
        return database.allStocks();
    }

    @GetMapping("/{symbol}")
    public Stock getStock(@PathVariable String symbol) {
        return database.getStock(symbol).orElseThrow(() -> new StockNotFoundException(symbol));
    }

    @GetMapping("/{symbol}/price")
    public PriceResponse getPrice(@PathVariable String symbol) {
        Stock stock = database.getStock(symbol).orElseThrow(() -> new StockNotFoundException(symbol));
        return new PriceResponse(stock.symbol, stock.price);
    }

    @PostMapping("/{symbol}/refresh")
    public PriceResponse refreshPrice(@PathVariable String symbol) {
        database.allStocks().forEach(s -> {
            double change = s.price * (Math.random() * 0.04 - 0.02);
            database.updatePrice(s.symbol, Math.max(1, s.price + change));
        });
        Stock stock = database.getStock(symbol).orElseThrow(() -> new StockNotFoundException(symbol));
        return new PriceResponse(stock.symbol, stock.price);
    }

    // Response DTOs
    public record PriceResponse(String symbol, double price) {}

    @ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    public static class StockNotFoundException extends RuntimeException {
        public StockNotFoundException(String symbol) {
            super("Stock not found: " + symbol);
        }
    }
}