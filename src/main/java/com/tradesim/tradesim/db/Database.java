package com.tradesim.tradesim.db;

import com.tradesim.tradesim.model.Holding;
import com.tradesim.tradesim.model.Order;
import com.tradesim.tradesim.model.Stock;
import com.tradesim.tradesim.model.User;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class Database {
    private Connection conn;

    @PostConstruct
    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:tradesim.db");
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys=ON");
                st.execute("CREATE TABLE IF NOT EXISTS users(id INTEGER PRIMARY KEY, username TEXT UNIQUE, password TEXT, cash REAL DEFAULT 10000)");
                st.execute("CREATE TABLE IF NOT EXISTS stocks(symbol TEXT PRIMARY KEY, name TEXT, price REAL, sector TEXT)");
                st.execute("CREATE TABLE IF NOT EXISTS holdings(user_id INT, symbol TEXT, qty INT, avg_price REAL, PRIMARY KEY(user_id, symbol))");
                st.execute("CREATE TABLE IF NOT EXISTS orders(id INTEGER PRIMARY KEY, user_id INT, symbol TEXT, side TEXT, type TEXT, qty INT, price REAL, status TEXT DEFAULT 'PENDING')");
                seed();
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void seed() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT OR IGNORE INTO users VALUES(1,'demo','demo123',10000)");
            st.execute("""
                INSERT OR IGNORE INTO stocks VALUES
                ('AAPL','Apple',175,'Tech'),('GOOGL','Google',142,'Tech'),
                ('MSFT','Microsoft',378,'Tech'),('TSLA','Tesla',248,'Auto'),
                ('AMZN','Amazon',155,'Retail')
            """);
        }
    }

    @PreDestroy
public void close() { try { if (conn != null) conn.close(); } catch (Exception ignored) {} }

    // User
    public Optional<User> findUser(String u, String p) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username=? AND password=?")) {
            ps.setString(1, u); ps.setString(2, p);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(new User(rs.getInt("id"), rs.getString("username"), rs.getDouble("cash")));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public void updateCash(int uid, double cash) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET cash=? WHERE id=?")) {
            ps.setDouble(1, cash); ps.setInt(2, uid); ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // Stocks
    public List<Stock> allStocks() {
        List<Stock> list = new ArrayList<>();
        try (ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM stocks")) {
            while (rs.next()) list.add(new Stock(rs.getString("symbol"), rs.getString("name"), rs.getDouble("price"), rs.getString("sector")));
        } catch (Exception e) { throw new RuntimeException(e); }
        return list;
    }

    public Optional<Stock> getStock(String sym) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM stocks WHERE symbol=?")) {
            ps.setString(1, sym);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(new Stock(rs.getString("symbol"), rs.getString("name"), rs.getDouble("price"), rs.getString("sector")));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public void updatePrice(String sym, double price) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE stocks SET price=? WHERE symbol=?")) {
            ps.setDouble(1, price); ps.setString(2, sym); ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // Holdings
    public List<Holding> getHoldings(int uid) {
        List<Holding> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT h.symbol, h.qty, h.avg_price, s.name, s.price
            FROM holdings h JOIN stocks s ON h.symbol=s.symbol
            WHERE h.user_id=? AND h.qty>0
        """)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(new Holding(rs.getString("symbol"), rs.getString("name"), rs.getInt("qty"),
                                         rs.getDouble("avg_price"), rs.getDouble("price")));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return list;
    }

    public void upsertHolding(int uid, String sym, int qty, double avg) {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO holdings VALUES(?,?,?,?)
            ON CONFLICT(user_id,symbol) DO UPDATE SET qty=excluded.qty, avg_price=excluded.avg_price
        """)) {
            ps.setInt(1, uid); ps.setString(2, sym); ps.setInt(3, qty); ps.setDouble(4, avg);
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void deleteHolding(int uid, String sym) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM holdings WHERE user_id=? AND symbol=?")) {
            ps.setInt(1, uid); ps.setString(2, sym); ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // Orders
    public int placeOrder(int uid, String sym, String side, String type, int qty, double price) {
        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO orders(user_id,symbol,side,type,qty,price) VALUES(?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, uid); ps.setString(2, sym); ps.setString(3, side);
            ps.setString(4, type); ps.setInt(5, qty); ps.setDouble(6, price);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) return k.getInt(1); }
        } catch (Exception e) { throw new RuntimeException(e); }
        return -1;
    }

    public List<Order> getOrders(int uid) {
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT * FROM orders WHERE user_id=? ORDER BY id DESC")) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(new Order(rs.getInt("id"), rs.getString("symbol"), rs.getString("side"),
                                       rs.getString("type"), rs.getInt("qty"), rs.getDouble("price"),
                                       rs.getString("status")));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return list;
    }

    public void fillOrder(int id, String status) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET status=? WHERE id=?")) {
            ps.setString(1, status); ps.setInt(2, id); ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}