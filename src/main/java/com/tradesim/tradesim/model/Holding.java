package com.tradesim.tradesim.model;
public class Holding {
    public final String symbol, name;
    public final int qty;
    public final double avgPrice, curPrice;
    public Holding(String symbol, String name, int qty, double avgPrice, double curPrice) {
        this.symbol=symbol; this.name=name; this.qty=qty; this.avgPrice=avgPrice; this.curPrice=curPrice;
    }
    public double value() { return qty * curPrice; }
    public double pnl() { return qty * (curPrice - avgPrice); }
    public String symbol() { return symbol; }
    public String name() { return name; }
}