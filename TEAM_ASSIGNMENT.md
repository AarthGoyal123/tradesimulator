# TradeSim - Team Assignment (4 Members)

**Project:** TradeSim - JavaFX + Spring Boot Trading Simulator  
**Total Classes:** 11 Java source files  
**Division Strategy:** Equal ownership by functional module + MainWindow tab ownership

---

## 📋 Assignment Overview

| Member | Role | Primary Classes | MainWindow Tab | Est. Lines |
|--------|------|-----------------|----------------|------------|
| **Member 1** | Market Data Engineer | `Stock.java`, `StockRestController.java`, `SpringBootApp.java` | **Market Tab** | ~280 |
| **Member 2** | Trading Engine Developer | `Order.java`, `TradeRestController.java`, `TradeSimApp.java` | **Trade Tab** | ~260 |
| **Member 3** | Portfolio & Analytics | `Holding.java`, `User.java`, `PortfolioRestController.java` | **Portfolio Tab** | ~240 |
| **Member 4** | Data Layer & History | `Database.java` | **Orders Tab + Login/Auth** | ~280 |

---

## 🎯 Member 1 — Market Data Engineer

### **Owned Classes (Primary Responsibility)**

| File | Purpose | Key Methods to Present |
|------|---------|------------------------|
| `src/main/java/com/tradesim/tradesim/model/Stock.java` | Stock data model | Constructor, `symbol()`, `name()`, `sector()` getters |
| `src/main/java/com/tradesim/tradesim/controller/StockRestController.java` | REST API for market data | `getAllStocks()`, `getStock()`, `getPrice()`, `refreshPrice()` |
| `src/main/java/com/tradesim/tradesim/SpringBootApp.java` | Spring Boot entry point | `main()` — bootstrap REST API |

### **MainWindow.java — Market Tab Ownership**

| Method | Lines | Description |
|--------|-------|-------------|
| `createMarketTab()` | ~60 | Builds Market Overview tab: stats cards, TableView with 5 columns, action buttons |
| `refreshMarketPrices()` | ~5 | Reloads stocks from DB, refreshes table |
| `simulateMarket()` | ~10 | Applies ±3% random walk to all stock prices, updates DB |
| `startPriceSimulation()` | ~20 | JavaFX Timeline: auto-simulates ±1% price changes every 5 seconds |
| `createStatCard()` | ~20 | Reusable stat card widget (used in Market & Portfolio tabs) |

### **Presentation Talking Points**
- **Stock Model**: Immutable symbol/name/sector, mutable price for simulation
- **REST Endpoints**: `GET /api/stocks`, `GET /api/stocks/{symbol}`, `GET /api/stocks/{symbol}/price`, `POST /api/stocks/{symbol}/refresh`
- **Real-time Simulation**: Background timeline updates SQLite + UI every 5s
- **Market Table**: ObservableList binding, auto-refresh on simulation

---

## 🎯 Member 2 — Trading Engine Developer

### **Owned Classes (Primary Responsibility)**

| File | Purpose | Key Methods to Present |
|------|---------|------------------------|
| `src/main/java/com/tradesim/tradesim/model/Order.java` | Order record | Constructor, getters for TableView binding |
| `src/main/java/com/tradesim/tradesim/controller/TradeRestController.java` | REST API for trading | `placeOrder()`, `getOrders()` |
| `src/main/java/com/tradesim/tradesim/TradeSimApp.java` | JavaFX + Spring Boot integration | `init()`, `start()`, `stop()` — lifecycle management |

### **MainWindow.java — Trade Tab Ownership**

| Method | Lines | Description |
|--------|-------|-------------|
| `createTradeTab()` | ~150 | Full order form: stock ComboBox, side/type selectors, qty/price fields, preview, validation |
| `fill()` | ~15 | Executes market order: updates cash, holdings (upsert/delete), order status, DB sync |
| `updateTradePreview()` | ~5 | Calculates estimated cost, fee (0.1%), total — updates preview panel |
| `showMsg()` | ~5 | Success/error message display with styling |

### **Presentation Talking Points**
- **Order Types**: MARKET (instant fill at current price) vs LIMIT (user-specified price)
- **Validation**: Cash balance check for BUY, qty > 0, limit price required for LIMIT orders
- **Fee Model**: 0.1% commission on every trade
- **Holdings Sync**: BUY → upsert holding; SELL → delete holding (simplified)
- **REST API**: `POST /api/trade/order` with OrderRequest DTO, `GET /api/trade/orders`

---

## 🎯 Member 3 — Portfolio & Analytics

### **Owned Classes (Primary Responsibility)**

| File | Purpose | Key Methods to Present |
|------|---------|------------------------|
| `src/main/java/com/tradesim/tradesim/model/Holding.java` | Position + P&L | `value()`, `pnl()` calculations, getters |
| `src/main/java/com/tradesim/tradesim/model/User.java` | User session | `id`, `name`, mutable `cash` |
| `src/main/java/com/tradesim/tradesim/controller/PortfolioRestController.java` | REST API for portfolio | `getPortfolio()` — aggregates cash, holdings, P&L |

### **MainWindow.java — Portfolio Tab Ownership**

| Method | Lines | Description |
|--------|-------|-------------|
| `createPortfolioTab()` | ~70 | Holdings table (7 columns), summary cards (Cash, Value, P&L, Day Change), refresh |
| `updatePortfolioSummary()` | ~15 | Recalculates total value, P&L, updates summary cards with green/red styling |
| `createSummaryItem()` | ~20 | Reusable summary card widget for portfolio metrics |
| `refreshCash()` | ~5 | Updates cash label after trades |

### **Presentation Talking Points**
- **P&L Calculation**: `qty × (currentPrice - avgPrice)` — unrealized P&L
- **Portfolio Value**: `Σ(qty × currentPrice)` across all holdings
- **Holdings Table**: 7 columns with P&L color coding (green/red via CSS classes)
- **REST Response**: PortfolioResponse DTO with cash, totalValue, totalPnl, holdings list
- **Auto-refresh**: Portfolio updates after every trade execution

---

## 🎯 Member 4 — Data Layer & Order History

### **Owned Classes (Primary Responsibility)**

| File | Purpose | Key Methods to Present |
|------|---------|------------------------|
| `src/main/java/com/tradesim/tradesim/db/Database.java` | SQLite data access | **All methods** — schema init, seed, CRUD for users/stocks/holdings/orders |

### **MainWindow.java — Orders Tab + Auth Ownership**

| Method | Lines | Description |
|--------|-------|-------------|
| `createOrdersTab()` | ~40 | Order history table (7 columns), status color coding, refresh |
| `login()` | ~80 | Login screen: username/password fields, validation, DB auth, animations |
| `showError()` | ~5 | Error label display helper |
| `shake()` | ~10 | Login card shake animation on failed auth |
| `fadeOut()` | ~10 | Fade transition from login to main UI |
| `refreshAllTabs()` | ~10 | Refreshes all 4 tabs' data from DB |
| `getStatusStyleClass()` | ~10 | Maps order status → CSS class (filled/pending/cancelled/partial) |
| `createTableCol()` | ~35 | Generic TableColumn factory with optional row styling |

### **Presentation Talking Points**
- **Schema Design**: 4 tables (users, stocks, holdings, orders) with FK constraints
- **Connection Lifecycle**: `@PostConstruct` init, `@PreDestroy` close, singleton Connection
- **Seeding**: Demo user + 5 stocks (AAPL, GOOGL, MSFT, TSLA, AMZN) on first run
- **Holdings UPSERT**: SQLite `ON CONFLICT` for atomic insert/update
- **Order History**: Full audit trail with status tracking (PENDING→FILLED/CANCELLED)
- **Login Security**: Parameterized queries, Optional<User> return, shake/fade UX

---

## 📊 Grading Rubric (Suggested)

| Criterion | Weight | How to Evaluate |
|-----------|--------|-----------------|
| **Code Ownership** | 30% | Can explain every method in their classes + tab |
| **Integration** | 25% | Shows how their tab calls Database/REST, handles errors |
| **Presentation** | 20% | Clear demo, explains design decisions, answers questions |
| **Complexity** | 15% | Handles the harder parts (simulation timeline, order fill logic, P&L math, DB schema) |
| **Collaboration** | 10% | References shared models, consistent code style, git history |

---

## 🔗 Shared Understanding (All Members)

All 4 members should be able to explain:
- **Model classes**: User, Stock, Holding, Order — purpose and fields
- **Database.java**: Connection setup, `findUser()`, `allStocks()`, `getHoldings()`, `placeOrder()`
- **MainWindow.java structure**: Login → Main tabs, cash label, tab navigation
- **Run Commands**: `mvn clean package -DskipTests && java -jar target/TradeSim-1.0.0.jar`
- **Login Credentials**: `demo` / `demo123`

---

## 📝 Git Commit Attribution (Recommended)

Each member should commit their files with clear messages:
```bash
# Member 1
git add src/main/java/com/tradesim/tradesim/model/Stock.java
git add src/main/java/com/tradesim/tradesim/controller/StockRestController.java
git add src/main/java/com/tradesim/tradesim/SpringBootApp.java
git commit -m "feat(market): Stock model + REST API + SpringBoot entry [Member 1]"

# Member 2
git add src/main/java/com/tradesim/tradesim/model/Order.java
git add src/main/java/com/tradesim/tradesim/controller/TradeRestController.java
git add src/main/java/com/tradesim/tradesim/TradeSimApp.java
git commit -m "feat(trade): Order model + Trade REST API + JavaFX-Spring bridge [Member 2]"

# Member 3
git add src/main/java/com/tradesim/tradesim/model/Holding.java
git add src/main/java/com/tradesim/tradesim/model/User.java
git add src/main/java/com/tradesim/tradesim/controller/PortfolioRestController.java
git commit -m "feat(portfolio): Holding/User models + Portfolio REST API [Member 3]"

# Member 4
git add src/main/java/com/tradesim/tradesim/db/Database.java
git commit -m "feat(data): Database layer - schema, seed, all CRUD [Member 4]"

# Shared MainWindow - each member commits their tab methods
```

---

## ✅ Verification Checklist

Before presentation, each member verifies:
- [ ] Their classes compile without errors
- [ ] Their tab renders correctly in the UI
- [ ] Their REST endpoints respond (test with curl/Postman)
- [ ] Database operations work for their domain
- [ ] They can explain **every line** of their assigned code
- [ ] They can trace a full flow: UI → Controller → Database → Model → UI

---

**Generated:** 2026-11-22  
**Project:** TradeSim  
**Total Source Files:** 11 Java classes  
**Division:** 4 equal ownership areas by functional module