package com.tradesim.tradesim.model;
public class User {
    public final int id;
    public final String name;
    public double cash;
    public User(int id, String name, double cash) { this.id=id; this.name=name; this.cash=cash; }
}