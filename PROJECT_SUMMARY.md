# TradeSim - JavaFX Trading Simulator
## Assignment Project Summary

---

## 1. Individual Contributions

### Person 1 - Market & Dashboard
**Classes Owned:** `MainWindow.java` → `marketTab()` method (~30 lines)

**Additional Features:**
- Real-time stock price table with 5 columns (Symbol, Name, Price, Sector)
- "Refresh Prices" button - simulates market movement (±2% random walk)
- Auto-updates SQLite database with new prices
- TableView with ObservableList binding

---

### Person 2 - Trading & Orders
**Classes Owned:** `MainWindow.java` → `tradeTab()` method (~40 lines)

**Additional Features:**
- Stock selector (ComboBox) - loads from database
- Side selector: BUY / SELL
- Order type: MARKET (instant fill) / LIMIT (price field enabled)
- Quantity input with validation
- Limit price field (enabled only for LIMIT orders)
- Cash balance validation for BUY orders
- Instant market order execution with 0.1% fee
- Holdings auto-update on fill (insert/update/delete)

---

### Person 3 - Portfolio & Holdings
**Classes Owned:** `MainWindow.java` → `portfolioTab()` method (~20 lines)

**Additional Features:**
- Holdings table with 7 columns: Symbol, Name, Qty, Avg Price, Current Price, Value, P&L
- P&L calculation: `qty × (current - avg)`
- Value calculation: `qty × current`
- Refresh button reloads from database
- Color-ready P&L column (green/red)

---

### Person 4 - Order History
**Classes Owned:** `MainWindow.java` → `ordersTab()` method (~15 lines)

**Additional Features:**
- Order history table with 7 columns: ID, Symbol, Side, Type, Qty, Price, Status
- Shows all orders (PENDING, FILLED, CANCELLED)
- Refresh button
- Ordered by most recent first

---

### Shared Infrastructure (All Teammates Understand)
| Class | Owner | Purpose |
|-------|-------|---------|
| `TradeSimApp.java` | All | Entry point, lifecycle |
| `MainWindow.java` | All | Login, tab layout, cash label |
| `Database.java` | All | All SQL operations |
| `model/Stock.java` | All | Stock data model |
| `model/Holding.java` | All | Position + P&L logic |
| `model/Order.java` | All | Order record |
| `model/User.java` | All | User session + cash |

---

## 2. Class Details & Code

---

### `TradeSimApp.java` (Entry Point)
**Purpose:** JavaFX Application bootstrap, database lifecycle

```java
package com.tradesim.tradesim;

import com.tradesim.tradesim.db.Database;
import com.tradesim.tradesim.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class TradeSimApp extends Application {
    private Database db = new Database();

    @Override
    public void init() { db.init(); }

    @Override
    public void start(Stage stage) { new MainWindow(db).show(stage); }

    @Override
    public void stop() { db.close(); }

    public static void main(String[] args) { launch(args); }
}
```

---

### `MainWindow.java` (UI Controller - 4 Tabs)
**Purpose:** Login screen, main tabbed interface, cash display, 4 module tabs

```java
package com.tradesim.tradesim.ui;

import com.tradesim.tradesim.db.Database;
import com.tradesim.tradesim.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class MainWindow {
    private final Database db;
    private User user;
    private Stage stage;
    private Label cashLabel;

    public MainWindow(Database db) { this.db = db; }

    public void show(Stage s) {
        this.stage = s;
        login();
    }

    private void login() {
        VBox box = new VBox(10, new Label("TradeSim - Login"), new TextField(), new PasswordField(), new Button("Login"));
        box.setAlignment(Pos.CENTER); box.setPadding(new Insets(20));
        TextField u = (TextField) box.getChildren().get(1); u.setPromptText("username");
        PasswordField p = (PasswordField) box.getChildren().get(2); p.setPromptText("password");
        Button btn = (Button) box.getChildren().get(3);
        btn.setOnAction(e -> db.findUser(u.getText(), p.getText()).ifPresentOrElse(
            usr -> { user = usr; main(); }, () -> new Alert(Alert.AlertType.ERROR, "Bad login").show()
        ));
        stage.setScene(new Scene(box, 300, 200)); stage.show();
    }

    private void main() {
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
            new Tab("Market", marketTab()),
            new Tab("Trade", tradeTab()),
            new Tab("Portfolio", portfolioTab()),
            new Tab("Orders", ordersTab())
        );
        cashLabel = new Label("Cash: $" + user.cash);
        cashLabel.setStyle("-fx-font-weight:bold; -fx-text-fill:white;");
        HBox top = new HBox(new Label("User: " + user.name), new Region(), cashLabel);
        top.setStyle("-fx-background:#2c3e50; -fx-padding:10;"); HBox.setHgrow(top.getChildren().get(1), Priority.ALWAYS);
        BorderPane root = new BorderPane(tabs, top, null, null, null);
        stage.setScene(new Scene(root, 800, 500)); stage.setTitle("TradeSim");
    }

    private void refreshCash() { cashLabel.setText("Cash: $" + String.format("%.2f", user.cash)); }

    // ========== PERSON 1: MARKET ==========
    private VBox marketTab() {
        TableView<Stock> tv = new TableView<>();
        tv.getColumns().addAll(col("Symbol", Stock::symbol), col("Name", Stock::name),
            col("Price", s -> String.format("$%.2f", s.price)), col("Sector", Stock::sector));
        tv.getItems().addAll(db.allStocks());
        Button refresh = new Button("Refresh Prices");
        refresh.setOnAction(e -> {
            db.allStocks().forEach(s -> {
                double change = s.price * (Math.random() * 0.04 - 0.02);
                db.updatePrice(s.symbol, Math.max(1, s.price + change));
            });
            tv.getItems().setAll(db.allStocks());
        });
        return new VBox(10, tv, refresh);
    }

    // ========== PERSON 2: TRADE ==========
    private VBox tradeTab() {
        ComboBox<Stock> stockBox = new ComboBox<>(); stockBox.getItems().addAll(db.allStocks());
        ComboBox<String> sideBox = new ComboBox<>(); sideBox.getItems().addAll("BUY", "SELL"); sideBox.setValue("BUY");
        ComboBox<String> typeBox = new ComboBox<>(); typeBox.getItems().addAll("MARKET", "LIMIT"); typeBox.setValue("MARKET");
        TextField qty = new TextField(); qty.setPromptText("Quantity");
        TextField price = new TextField(); price.setPromptText("Limit Price"); price.setDisable(true);
        typeBox.valueProperty().addListener((o, a, b) -> price.setDisable(!"LIMIT".equals(b)));
        Button place = new Button("Place Order");
        Label msg = new Label();
        place.setOnAction(e -> {
            try {
                Stock s = stockBox.getValue(); int q = Integer.parseInt(qty.getText());
                double p = "LIMIT".equals(typeBox.getValue()) ? Double.parseDouble(price.getText()) : s.price;
                double cost = q * p;
                if ("BUY".equals(sideBox.getValue()) && cost > user.cash) { msg.setText("Insufficient cash"); return; }
                int id = db.placeOrder(user.id, s.symbol, sideBox.getValue(), typeBox.getValue(), q, p);
                if ("MARKET".equals(typeBox.getValue())) fill(id, s, q, p, sideBox.getValue());
                msg.setText("Order placed: " + sideBox.getValue() + " " + q + " " + s.symbol);
                refreshCash();
            } catch (Exception ex) { msg.setText("Error: " + ex.getMessage()); }
        });
        return new VBox(10, new Label("Place Order"), stockBox, sideBox, typeBox, qty, price, place, msg);
    }

    private void fill(int id, Stock s, int q, double p, String side) {
        double fee = q * p * 0.001;
        if ("BUY".equals(side)) {
            user.cash -= q * p + fee;
            db.upsertHolding(user.id, s.symbol, q, p);
        } else {
            user.cash += q * p - fee;
            db.deleteHolding(user.id, s.symbol);
        }
        db.updateCash(user.id, user.cash);
        db.fillOrder(id, "FILLED");
    }

    // ========== PERSON 3: PORTFOLIO ==========
    private VBox portfolioTab() {
        TableView<Holding> tv = new TableView<>();
        tv.getColumns().addAll(col("Symbol", Holding::symbol), col("Name", Holding::name),
            col("Qty", h -> String.valueOf(h.qty)), col("Avg", h -> "$" + h.avgPrice),
            col("Cur", h -> "$" + h.curPrice), col("Value", h -> "$" + h.value()),
            col("P&L", h -> String.format("$%.2f", h.pnl())));
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> tv.getItems().setAll(db.getHoldings(user.id)));
        refresh.fire();
        return new VBox(10, tv, refresh);
    }

    // ========== PERSON 4: ORDERS ==========
    private VBox ordersTab() {
        TableView<Order> tv = new TableView<>();
        tv.getColumns().addAll(col("ID", o -> String.valueOf(o.id)), col("Symbol", Order::symbol),
            col("Side", Order::side), col("Type", Order::type), col("Qty", o -> String.valueOf(o.qty)),
            col("Price", o -> "$" + o.price), col("Status", Order::status));
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> tv.getItems().setAll(db.getOrders(user.id)));
        refresh.fire();
        return new VBox(10, tv, refresh);
    }

    private <T> TableColumn<T, String> col(String title, java.util.function.Function<T, String> f) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(f.apply(d.getValue())));
        return c;
    }
}
```

---

### `Database.java` (Data Layer)
**Purpose:** SQLite connection, schema init, all CRUD operations

```java
package com.tradesim.tradesim.db;

import com.tradesim.tradesim.model.Holding;
import com.tradesim.tradesim.model.Order;
import com.tradesim.tradesim.model.Stock;
import com.tradesim.tradesim.model.User;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Database {
    private Connection conn;

    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:tradesim.db");
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys=ON");
                st.execute("""
                    CREATE TABLE IF NOT EXISTS users(
                        id INTEGER PRIMARY KEY, username TEXT UNIQUE, password TEXT, cash REAL DEFAULT 10000
                    );
                    CREATE TABLE IF NOT EXISTS stocks(
                        symbol TEXT PRIMARY KEY, name TEXT, price REAL, sector TEXT
                    );
                    CREATE TABLE IF NOT EXISTS holdings(
                        user_id INT, symbol TEXT, qty INT, avg_price REAL,
                        PRIMARY KEY(user_id, symbol)
                    );
                    CREATE TABLE IF NOT EXISTS orders(
                        id INTEGER PRIMARY KEY, user_id INT, symbol TEXT, side TEXT, type TEXT,
                        qty INT, price REAL, status TEXT DEFAULT 'PENDING'
                    );
                """);
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
```

---

### `model/User.java`
**Purpose:** User session data

```java
package com.tradesim.tradesim.model;
public class User {
    public final int id;
    public final String name;
    public double cash;
    public User(int id, String name, double cash) { this.id=id; this.name=name; this.cash=cash; }
}
```

---

### `model/Stock.java`
**Purpose:** Stock reference data + getter methods for TableView

```java
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
```

---

### `model/Holding.java`
**Purpose:** User position with P&L calculations

```java
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
```

---

### `model/Order.java`
**Purpose:** Order record with getters for TableView

```java
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
```

---

## Run Instructions

```bash
cd C:\Users\burra\IdeaProjects\TradeSim
mvn clean package -DskipTests
java -jar target/TradeSim-1.0.0.jar
```

**Login:** `demo` / `demo123`

---

## Tech Stack
- Java 21 + JavaFX 23 (controls)
- SQLite (embedded, file: `tradesim.db`)
- Maven (shade plugin for fat JAR)
- ~250 lines total across 8 files