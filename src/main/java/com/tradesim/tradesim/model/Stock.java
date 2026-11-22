package com.tradesim.tradesim.model;
public class Stock {
    public final String symbol, name, sector;
    public double price;
    public Stock(String symbol, String name, double price, String sector) {
        this.symbol=symbol; this.name=name; this.price=price; this.sector=sector;
    }
    public String symbol() { return symbol; }
    public String name() { return name; }
    public String sector() { return sector; }
}