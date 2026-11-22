package com.tradesim.tradesim.model;
public class Order {
    public final int id;
    public final String symbol, side, type, status;
    public final int qty;
    public final double price;
    public Order(int id, String symbol, String side, String type, int qty, double price, String status) {
        this.id=id; this.symbol=symbol; this.side=side; this.type=type; this.qty=qty; this.price=price; this.status=status;
    }
    public String symbol() { return symbol; }
    public String side() { return side; }
    public String type() { return type; }
    public String status() { return status; }
}