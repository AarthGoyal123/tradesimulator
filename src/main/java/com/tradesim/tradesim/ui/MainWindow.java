package com.tradesim.tradesim.ui;

import com.tradesim.tradesim.db.Database;
import com.tradesim.tradesim.model.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

public class MainWindow {
    private final Database db;
    private User user;
    private Stage stage;
    private Label cashLabel;
    private Label portfolioValueLabel;
    private Label totalPnlLabel;
    private Label dayChangeLabel;
    private ObservableList<Stock> marketStocks = FXCollections.observableArrayList();
    private ObservableList<Holding> holdings = FXCollections.observableArrayList();
    private ObservableList<Order> orders = FXCollections.observableArrayList();
    private TableView<Stock> marketTable;
    private TableView<Holding> portfolioTable;
    private TableView<Order> ordersTable;
    private final NumberFormat currencyFmt = NumberFormat.getCurrencyInstance(Locale.US);
    private final NumberFormat percentFmt = NumberFormat.getPercentInstance(Locale.US);
    private boolean isDarkTheme = false;

    public MainWindow(Database db) { this.db = db; }

    public void show(Stage s) {
        this.stage = s;
        applyTheme();
        login();
    }

    private void applyTheme() {
        String theme = isDarkTheme ? "dark" : "light";
        if (stage != null && stage.getScene() != null) {
            stage.getScene().getRoot().setStyle("-fx-theme: " + theme + ";");
        }
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        applyTheme();
        // Re-apply styles by refreshing scene
        Scene scene = stage.getScene();
        if (scene != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        }
    }

    private void login() {
        VBox root = new VBox(0);
        root.getStyleClass().add("login-container");

        VBox card = new VBox(24);
        card.getStyleClass().add("login-card");

        Label title = new Label("TradeSim");
        title.getStyleClass().add("login-title");

        Label subtitle = new Label("Trading Simulator — Demo Mode");
        subtitle.getStyleClass().add("login-subtitle");

        VBox fields = new VBox(16);
        fields.getStyleClass().add("login-field-group");

        // Username
        VBox userGroup = new VBox(6);
        Label userLabel = new Label("Username");
        userLabel.getStyleClass().add("login-field-label");
        TextField userField = new TextField();
        userField.setPromptText("demo");
        userField.getStyleClass().add("input-field");
        userField.setText("demo");
        userGroup.getChildren().addAll(userLabel, userField);

        // Password
        VBox passGroup = new VBox(6);
        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("login-field-label");
        PasswordField passField = new PasswordField();
        passField.setPromptText("demo123");
        passField.getStyleClass().add("input-field");
        passField.setText("demo123");
        passGroup.getChildren().addAll(passLabel, passField);

        // Error label (hidden initially)
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("login-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Login button
        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().addAll("btn", "btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setDefaultButton(true);

        loginBtn.setOnAction(e -> {
            String u = userField.getText().trim();
            String p = passField.getText();
            if (u.isEmpty() || p.isEmpty()) {
                showError(errorLabel, "Please enter both username and password");
                return;
            }
            Optional<User> opt = db.findUser(u, p);
            if (opt.isPresent()) {
                user = opt.get();
                fadeOut(root, () -> main());
            } else {
                showError(errorLabel, "Invalid username or password");
                shake(card);
            }
        });

        // Enter key support
        passField.setOnAction(e -> loginBtn.fire());

        fields.getChildren().addAll(userGroup, passGroup, errorLabel, loginBtn);
        card.getChildren().addAll(title, subtitle, fields);
        root.getChildren().add(card);

        Scene scene = new Scene(root, 480, 560);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("TradeSim - Login");
        stage.centerOnScreen();
        stage.show();

        // Focus username field
        Platform.runLater(() -> userField.requestFocus());
    }

    private void showError(Label errorLabel, String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(-10);
        tt.setToX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }

    private void fadeOut(Node node, Runnable onFinished) {
        FadeTransition ft = new FadeTransition(Duration.millis(200), node);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> onFinished.run());
        ft.play();
    }

    private void main() {
        // Initialize data
        marketStocks.setAll(db.allStocks());
        holdings.setAll(db.getHoldings(user.id));
        orders.setAll(db.getOrders(user.id));

        // Top Bar
        HBox topBar = new HBox(16);
        topBar.getStyleClass().add("top-bar");

        Label title = new Label("TradeSim");
        title.getStyleClass().add("title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label("Logged in as: " + user.name);
        userLabel.getStyleClass().add("user-info");

        cashLabel = new Label(currencyFmt.format(user.cash));
        cashLabel.getStyleClass().add("cash-display");

        Button themeBtn = new Button(isDarkTheme ? "☀ Light" : "🌙 Dark");
        themeBtn.getStyleClass().add("theme-toggle");
        themeBtn.setOnAction(e -> {
            isDarkTheme = !isDarkTheme;
            themeBtn.setText(isDarkTheme ? "☀ Light" : "🌙 Dark");
            applyTheme();
            refreshSceneStyles();
        });

        topBar.getChildren().addAll(title, spacer, userLabel, cashLabel, themeBtn);

        // TabPane
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab marketTab = new Tab("Market");
        marketTab.setContent(createMarketTab());
        marketTab.setClosable(false);

        Tab tradeTab = new Tab("Trade");
        tradeTab.setContent(createTradeTab());
        tradeTab.setClosable(false);

        Tab portfolioTab = new Tab("Portfolio");
        portfolioTab.setContent(createPortfolioTab());
        portfolioTab.setClosable(false);

        Tab ordersTab = new Tab("Orders");
        ordersTab.setContent(createOrdersTab());
        ordersTab.setClosable(false);

        tabs.getTabs().addAll(marketTab, tradeTab, portfolioTab, ordersTab);

        BorderPane root = new BorderPane(tabs, topBar, null, null, null);
        root.setStyle("-fx-background-color: var(--bg-primary);");

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("TradeSim — Trading Simulator");
        stage.centerOnScreen();
        stage.show();

        // Start price simulation
        startPriceSimulation();
    }

    private void refreshSceneStyles() {
        Scene scene = stage.getScene();
        if (scene != null) {
            String css = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(css);
        }
    }

    private void refreshCash() {
        Platform.runLater(() -> {
            cashLabel.setText(currencyFmt.format(user.cash));
            if (portfolioValueLabel != null) updatePortfolioSummary();
        });
    }

    private void updatePortfolioSummary() {
        double totalValue = holdings.stream().mapToDouble(Holding::value).sum();
        double totalPnl = holdings.stream().mapToDouble(Holding::pnl).sum();
        double dayChange = 0; // Could calculate from price changes

        portfolioValueLabel.setText(currencyFmt.format(totalValue));
        totalPnlLabel.setText(currencyFmt.format(totalPnl));
        totalPnlLabel.getStyleClass().removeAll("label-success", "label-danger", "label-warning");
        totalPnlLabel.getStyleClass().add(totalPnl >= 0 ? "label-success" : "label-danger");

        dayChangeLabel.setText(percentFmt.format(dayChange));
    }

    // ==================== MARKET TAB ====================
    private VBox createMarketTab() {
        VBox container = new VBox(16);
        container.getStyleClass().add("card");
        container.setPadding(new Insets(24));

        Label title = new Label("Market Overview");
        title.getStyleClass().add("card-title");

        // Stats row
        HBox statsRow = new HBox(16);
        statsRow.getStyleClass().add("stats-grid");
        statsRow.setAlignment(Pos.CENTER_LEFT);

        statsRow.getChildren().addAll(
            createStatCard("Stocks", String.valueOf(marketStocks.size()), "Available"),
            createStatCard("Gainers", "—", "Today"),
            createStatCard("Losers", "—", "Today"),
            createStatCard("Volume", "—", "24h")
        );

        // Market Table
        marketTable = new TableView<>(marketStocks);
        marketTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        marketTable.setRowFactory(tv -> new TableRow<Stock>() {
            @Override
            protected void updateItem(Stock item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    // Could add price change indicators here
                }
            }
        });

        marketTable.getColumns().addAll(
            createTableCol("Symbol", Stock::symbol, false),
            createTableCol("Company", Stock::name, false),
            createTableCol("Price", s -> currencyFmt.format(s.price), true),
            createTableCol("Sector", Stock::sector, false),
            createTableCol("Change", s -> "—", true) // Would need previous price
        );

        // Action buttons
        HBox actions = new HBox(12);
        Button refreshBtn = new Button("Refresh Prices");
        refreshBtn.getStyleClass().addAll("btn", "btn-secondary");
        refreshBtn.setOnAction(e -> refreshMarketPrices());

        Button simulateBtn = new Button("Simulate Market");
        simulateBtn.getStyleClass().addAll("btn", "btn-primary");
        simulateBtn.setOnAction(e -> simulateMarket());

        actions.getChildren().addAll(refreshBtn, simulateBtn);

        container.getChildren().addAll(title, statsRow, marketTable, actions);
        VBox.setVgrow(marketTable, Priority.ALWAYS);

        return container;
    }

    private void refreshMarketPrices() {
        marketStocks.setAll(db.allStocks());
        marketTable.refresh();
    }

    private void simulateMarket() {
        for (Stock s : marketStocks) {
            double change = s.price * (Math.random() * 0.06 - 0.03); // ±3%
            double newPrice = Math.max(1, s.price + change);
            db.updatePrice(s.symbol, newPrice);
        }
        refreshMarketPrices();
        updatePortfolioSummary();
    }

    // ==================== TRADE TAB ====================
    private VBox createTradeTab() {
        VBox container = new VBox(20);
        container.getStyleClass().add("card");
        container.setPadding(new Insets(24));

        Label title = new Label("Place Order");
        title.getStyleClass().add("card-title");

        // Form grid
        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(16);
        form.getStyleClass().add("trade-form");

        // Row 0: Symbol
        Label symLabel = new Label("Symbol");
        symLabel.getStyleClass().add("form-group");
        ComboBox<Stock> stockBox = new ComboBox<>(marketStocks);
        stockBox.setConverter(new javafx.util.StringConverter<Stock>() {
            @Override public String toString(Stock s) { return s != null ? s.symbol + " — " + s.name : ""; }
            @Override public Stock fromString(String s) { return null; }
        });
        stockBox.getStyleClass().add("input-field");
        stockBox.setPrefWidth(300);

        // Row 1: Side & Type
        Label sideLabel = new Label("Side");
        sideLabel.getStyleClass().add("form-group");
        ComboBox<String> sideBox = new ComboBox<>(FXCollections.observableArrayList("BUY", "SELL"));
        sideBox.setValue("BUY");
        sideBox.getStyleClass().add("input-field");

        Label typeLabel = new Label("Order Type");
        typeLabel.getStyleClass().add("form-group");
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("MARKET", "LIMIT"));
        typeBox.setValue("MARKET");
        typeBox.getStyleClass().add("input-field");

        // Row 2: Quantity & Price
        Label qtyLabel = new Label("Quantity");
        qtyLabel.getStyleClass().add("form-group");
        TextField qtyField = new TextField();
        qtyField.setPromptText("e.g., 10");
        qtyField.getStyleClass().add("input-field");

        Label priceLabel = new Label("Limit Price");
        priceLabel.getStyleClass().add("form-group");
        TextField priceField = new TextField();
        priceField.setPromptText("Required for LIMIT orders");
        priceField.getStyleClass().add("input-field");
        priceField.setDisable(true);

        typeBox.valueProperty().addListener((obs, old, val) -> {
            priceField.setDisable(!"LIMIT".equals(val));
            if ("MARKET".equals(val)) priceField.clear();
            updateTradePreview();
        });

        // Row 3: Preview
        VBox previewBox = new VBox(8);
        previewBox.getStyleClass().add("trade-preview");
        previewBox.setVisible(false);
        previewBox.setManaged(false);

        Label previewTitle = new Label("Order Preview");
        previewTitle.setStyle("-fx-font-weight: 600; -fx-text-fill: var(--text-secondary);");

        HBox previewRow1 = new HBox(12);
        previewRow1.getStyleClass().add("trade-preview-row");
        previewRow1.getChildren().addAll(
            new Label("Est. Cost:"), new Label("—")
        );
        ((Label)previewRow1.getChildren().get(1)).getStyleClass().addAll("trade-preview-value");

        HBox previewRow2 = new HBox(12);
        previewRow2.getStyleClass().add("trade-preview-row");
        previewRow2.getChildren().addAll(
            new Label("Fee (0.1%):"), new Label("—")
        );
        ((Label)previewRow2.getChildren().get(1)).getStyleClass().addAll("trade-preview-value");

        HBox previewRow3 = new HBox(12);
        previewRow3.getStyleClass().add("trade-preview-row");
        previewRow3.getChildren().addAll(
            new Label("Total:"), new Label("—")
        );
        ((Label)previewRow3.getChildren().get(1)).getStyleClass().addAll("trade-preview-total");

        previewBox.getChildren().addAll(previewTitle, previewRow1, previewRow2, previewRow3);

        // Bind preview updates
        Runnable updatePreview = this::updateTradePreview;
        stockBox.valueProperty().addListener((o,a,b) -> updatePreview.run());
        sideBox.valueProperty().addListener((o,a,b) -> updatePreview.run());
        typeBox.valueProperty().addListener((o,a,b) -> updatePreview.run());
        qtyField.textProperty().addListener((o,a,b) -> updatePreview.run());
        priceField.textProperty().addListener((o,a,b) -> updatePreview.run());

        // Place button + message
        Button placeBtn = new Button("Place Order");
        placeBtn.getStyleClass().addAll("btn", "btn-primary");
        placeBtn.setMaxWidth(Double.MAX_VALUE);

        Label msgLabel = new Label();
        msgLabel.setStyle("-fx-font-size: 13px;");
        msgLabel.setWrapText(true);

        placeBtn.setOnAction(e -> {
            try {
                Stock s = stockBox.getValue();
                if (s == null) { showMsg(msgLabel, "Select a stock", true); return; }

                int q = Integer.parseInt(qtyField.getText().trim());
                if (q <= 0) { showMsg(msgLabel, "Quantity must be positive", true); return; }

                String side = sideBox.getValue();
                String type = typeBox.getValue();
                double p = "LIMIT".equals(type) ? Double.parseDouble(priceField.getText().trim()) : s.price;
                if ("LIMIT".equals(type) && p <= 0) { showMsg(msgLabel, "Enter a valid limit price", true); return; }

                double cost = q * p;
                double fee = cost * 0.001;
                double total = cost + fee;

                if ("BUY".equals(side) && total > user.cash) {
                    showMsg(msgLabel, "Insufficient cash: need " + currencyFmt.format(total) + ", have " + currencyFmt.format(user.cash), true);
                    return;
                }

                int id = db.placeOrder(user.id, s.symbol, side, type, q, p);
                if ("MARKET".equals(type)) fill(id, s, q, p, side);

                showMsg(msgLabel, "✓ Order placed: " + side + " " + q + " " + s.symbol + " @ " + currencyFmt.format(p), false);
                refreshAllTabs();
            } catch (NumberFormatException ex) {
                showMsg(msgLabel, "Invalid number format", true);
            } catch (Exception ex) {
                showMsg(msgLabel, "Error: " + ex.getMessage(), true);
            }
        });

        // Layout form
        form.add(symLabel, 0, 0); form.add(stockBox, 1, 0);
        form.add(sideLabel, 0, 1); form.add(sideBox, 1, 1);
        form.add(typeLabel, 2, 1); form.add(typeBox, 3, 1);
        form.add(qtyLabel, 0, 2); form.add(qtyField, 1, 2);
        form.add(priceLabel, 2, 2); form.add(priceField, 3, 2);

        container.getChildren().addAll(title, form, previewBox, placeBtn, msgLabel);
        return container;
    }

    private void updateTradePreview() {
        // Find preview box in scene
        // Simplified: just update when needed in placeBtn action
    }

    private void showMsg(Label label, String msg, boolean isError) {
        label.setText(msg);
        label.getStyleClass().removeAll("label-success", "label-danger");
        label.getStyleClass().add(isError ? "label-danger" : "label-success");
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

    // ==================== PORTFOLIO TAB ====================
    private VBox createPortfolioTab() {
        VBox container = new VBox(16);
        container.getStyleClass().add("card");
        container.setPadding(new Insets(24));

        Label title = new Label("Portfolio");
        title.getStyleClass().add("card-title");

        // Summary cards
        HBox summary = new HBox(24);
        summary.getStyleClass().add("portfolio-summary");
        summary.setAlignment(Pos.CENTER_LEFT);

        VBox cashCard = createSummaryItem("Cash", () -> currencyFmt.format(user.cash), null);
        VBox valueCard = createSummaryItem("Portfolio Value", () -> {
            double v = holdings.stream().mapToDouble(Holding::value).sum();
            return currencyFmt.format(v);
        }, null);
        VBox pnlCard = createSummaryItem("Total P&L", () -> {
            double p = holdings.stream().mapToDouble(Holding::pnl).sum();
            return currencyFmt.format(p);
        }, () -> {
            double p = holdings.stream().mapToDouble(Holding::pnl).sum();
            totalPnlLabel.getStyleClass().removeAll("label-success", "label-danger");
            totalPnlLabel.getStyleClass().add(p >= 0 ? "label-success" : "label-danger");
        });

        portfolioValueLabel = (Label) valueCard.getChildren().get(0);
        totalPnlLabel = (Label) pnlCard.getChildren().get(0);
        dayChangeLabel = new Label("—");
        dayChangeLabel.getStyleClass().add("summary-value");

        VBox dayCard = createSummaryItem("Day Change", () -> "—", null);
        dayChangeLabel = (Label) dayCard.getChildren().get(0);

        summary.getChildren().addAll(cashCard, valueCard, pnlCard, dayCard);

        // Holdings Table
        portfolioTable = new TableView<>(holdings);
        portfolioTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        portfolioTable.setRowFactory(tv -> new TableRow<Holding>() {
            @Override
            protected void updateItem(Holding item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setStyle("");
            }
        });

        portfolioTable.getColumns().addAll(
            createTableCol("Symbol", Holding::symbol, false),
            createTableCol("Company", Holding::name, false),
            createTableCol("Qty", h -> String.valueOf(h.qty), true),
            createTableCol("Avg Price", h -> currencyFmt.format(h.avgPrice), true),
            createTableCol("Current", h -> currencyFmt.format(h.curPrice), true),
            createTableCol("Value", h -> currencyFmt.format(h.value()), true),
            createTableCol("P&L", h -> currencyFmt.format(h.pnl()), true, h -> h.pnl() >= 0 ? "holding-positive" : "holding-negative")
        );

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().addAll("btn", "btn-secondary");
        refreshBtn.setOnAction(e -> {
            holdings.setAll(db.getHoldings(user.id));
            updatePortfolioSummary();
            refreshCash();
        });

        container.getChildren().addAll(title, summary, portfolioTable, refreshBtn);
        VBox.setVgrow(portfolioTable, Priority.ALWAYS);

        return container;
    }

    private VBox createSummaryItem(String label, Supplier<String> valueSupplier, Runnable onUpdate) {
        VBox box = new VBox(4);
        box.getStyleClass().add("summary-item");
        box.setAlignment(Pos.CENTER);
        box.setMinWidth(180);

        Label value = new Label(valueSupplier.get());
        value.getStyleClass().add("summary-value");
        value.setFont(Font.font("JetBrains Mono", 24));

        Label lbl = new Label(label);
        lbl.getStyleClass().add("summary-label");

        box.getChildren().addAll(value, lbl);

        if (onUpdate != null) {
            // Store reference for updates
        }

        return box;
    }

    // ==================== ORDERS TAB ====================
    private VBox createOrdersTab() {
        VBox container = new VBox(16);
        container.getStyleClass().add("card");
        container.setPadding(new Insets(24));

        Label title = new Label("Order History");
        title.getStyleClass().add("card-title");

        ordersTable = new TableView<>(orders);
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        ordersTable.setRowFactory(tv -> new TableRow<Order>() {
            @Override
            protected void updateItem(Order item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setStyle("");
            }
        });

        ordersTable.getColumns().addAll(
            createTableCol("ID", o -> String.valueOf(o.id), true),
            createTableCol("Symbol", Order::symbol, false),
            createTableCol("Side", Order::side, false, o -> "BUY".equals(o.side) ? "label-success" : "label-danger"),
            createTableCol("Type", Order::type, false),
            createTableCol("Qty", o -> String.valueOf(o.qty), true),
            createTableCol("Price", o -> currencyFmt.format(o.price), true),
            createTableCol("Status", Order::status, false, this::getStatusStyleClass)
        );

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().addAll("btn", "btn-secondary");
        refreshBtn.setOnAction(e -> orders.setAll(db.getOrders(user.id)));

        container.getChildren().addAll(title, ordersTable, refreshBtn);
        VBox.setVgrow(ordersTable, Priority.ALWAYS);

        return container;
    }

    private String getStatusStyleClass(Order o) {
        return switch (o.status) {
            case "FILLED" -> "status-filled";
            case "PENDING" -> "status-pending";
            case "CANCELLED" -> "status-cancelled";
            case "PARTIAL" -> "status-partial";
            default -> "";
        };
    }

    // ==================== HELPERS ====================
    private <T> TableColumn<T, String> createTableCol(String title, java.util.function.Function<T, String> mapper, boolean numeric) {
        return createTableCol(title, mapper, numeric, null);
    }

    private <T> TableColumn<T, String> createTableCol(String title, java.util.function.Function<T, String> mapper, boolean numeric, java.util.function.Function<T, String> rowStyle) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cellData -> new SimpleStringProperty(mapper.apply(cellData.getValue())));
        if (numeric) col.getStyleClass().add("numeric-column");
        if (rowStyle != null) {
            col.setCellFactory(c -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        getStyleClass().removeAll("label-success", "label-danger", "holding-positive", "holding-negative",
                                "status-filled", "status-pending", "status-cancelled", "status-partial",
                                "price-up", "price-down");
                    } else {
                        setText(item);
                        TableRow<T> row = getTableRow();
                        if (row != null && row.getItem() != null) {
                            String style = rowStyle.apply(row.getItem());
                            if (style != null && !style.isEmpty()) {
                                getStyleClass().removeAll("label-success", "label-danger", "holding-positive", "holding-negative",
                                        "status-filled", "status-pending", "status-cancelled", "status-partial",
                                        "price-up", "price-down");
                                getStyleClass().add(style);
                            }
                        }
                    }
                }
            });
        }
        return col;
    }

    private VBox createStatCard(String title, String value, String subtitle) {
        VBox card = new VBox(4);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinWidth(160);

        Label val = new Label(value);
        val.getStyleClass().add("stat-value");
        val.setFont(Font.font("JetBrains Mono", 28));

        Label lbl = new Label(title);
        lbl.getStyleClass().add("stat-label");

        card.getChildren().addAll(val, lbl);
        if (subtitle != null) {
            Label sub = new Label(subtitle);
            sub.getStyleClass().add("label-muted");
            sub.setFont(Font.font(11));
            card.getChildren().add(sub);
        }
        return card;
    }

    private void refreshAllTabs() {
        marketStocks.setAll(db.allStocks());
        holdings.setAll(db.getHoldings(user.id));
        orders.setAll(db.getOrders(user.id));
        refreshCash();
        updatePortfolioSummary();
        if (marketTable != null) marketTable.refresh();
        if (portfolioTable != null) portfolioTable.refresh();
        if (ordersTable != null) ordersTable.refresh();
    }

    private void startPriceSimulation() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (stage != null && stage.isShowing()) {
                for (Stock s : marketStocks) {
                    double change = s.price * (Math.random() * 0.02 - 0.01); // ±1%
                    double newPrice = Math.max(1, s.price + change);
                    db.updatePrice(s.symbol, newPrice);
                }
                Platform.runLater(() -> {
                    marketStocks.setAll(db.allStocks());
                    if (marketTable != null) marketTable.refresh();
                    updatePortfolioSummary();
                });
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    @FunctionalInterface
    private interface Supplier<T> { T get(); }
}