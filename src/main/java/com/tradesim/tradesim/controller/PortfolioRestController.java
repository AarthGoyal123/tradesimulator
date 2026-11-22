package com.tradesim.tradesim.controller;

import com.tradesim.tradesim.db.Database;
import com.tradesim.tradesim.model.Holding;
import com.tradesim.tradesim.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Portfolio Data.
 * Shows Spring Boot exposing portfolio via HTTP.
 */
@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "*")
public class PortfolioRestController {

    private final Database database;

    @Autowired
    public PortfolioRestController(Database database) {
        this.database = database;
    }

    @GetMapping
    public PortfolioResponse getPortfolio() {
        int userId = 1;
        List<Holding> holdings = database.getHoldings(userId);
        User user = database.findUser("demo", "demo123").orElse(null);

        double totalValue = holdings.stream().mapToDouble(Holding::value).sum();
        double totalPnl = holdings.stream().mapToDouble(Holding::pnl).sum();
        double cash = user != null ? user.cash : 0;

        return new PortfolioResponse(cash, totalValue, totalPnl, holdings);
    }

    public record PortfolioResponse(double cash, double totalValue, double totalPnl, List<Holding> holdings) {}
}