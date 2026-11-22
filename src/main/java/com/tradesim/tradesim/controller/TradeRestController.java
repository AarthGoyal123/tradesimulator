package com.tradesim.tradesim.controller;

import com.tradesim.tradesim.db.Database;
import com.tradesim.tradesim.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Trading Operations.
 * Demonstrates Spring Boot handling trade requests via HTTP.
 */
@RestController
@RequestMapping("/api/trade")
@CrossOrigin(origins = "*")
public class TradeRestController {

    private final Database database;

    @Autowired
    public TradeRestController(Database database) {
        this.database = database;
    }

    @PostMapping("/order")
    public OrderResponse placeOrder(@RequestBody OrderRequest request) {
        // Simplified: assume user ID 1 (demo user)
        int userId = 1;
        double price = "LIMIT".equals(request.type()) ? request.price() : database.getStock(request.symbol()).map(s -> s.price).orElse(0.0);
        int orderId = database.placeOrder(userId, request.symbol(), request.side(), request.type(), request.qty(), price);

        if ("MARKET".equals(request.type())) {
            database.getStock(request.symbol()).ifPresent(stock -> {
                // Simplified fill logic
                database.fillOrder(orderId, "FILLED");
            });
        }

        return new OrderResponse(orderId, "PLACED");
    }

    @GetMapping("/orders")
    public List<Order> getOrders() {
        return database.getOrders(1);
    }

    // Request/Response DTOs
    public record OrderRequest(String symbol, String side, String type, int qty, double price) {}
    public record OrderResponse(int orderId, String status) {}
}