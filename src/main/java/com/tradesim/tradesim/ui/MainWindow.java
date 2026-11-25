package com.tradesim.tradesim.ui;

import com.tradesim.tradesim.db.Database;
import com.tradesim.tradesim.model.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class MainWindow {
    private final Database db;
    private User user;
    private Stage stage;
    private StackPane rootOverlay;
    private BorderPane mainLayout;

    // Top KPI labels
    private Label netWorthValLabel;
    private Label cashValLabel;
    private Label portfolioValLabel;
    private Label totalPnlValLabel;
    private Label totalPnlSubLabel;

    // Data lists
    private final ObservableList<Stock> marketStocks = FXCollections.observableArrayList();
    private FilteredList<Stock> filteredMarketStocks;
    private final ObservableList<Holding> holdings = FXCollections.observableArrayList();
    private final ObservableList<Order> orders = FXCollections.observableArrayList();

    // Tables
    private TableView<Stock> marketTable;
    private TableView<Holding> portfolioTable;
    private TableView<Order> ordersTable;

    // View Switching in Left Workspace
    private StackPane leftContentStack;
    private VBox marketViewPane;
    private VBox portfolioViewPane;
    private VBox ordersViewPane;
    private Button navMarketBtn;
    private Button navPortfolioBtn;
    private Button navOrdersBtn;

    // Order Ticket Components (Right Dock)
    private ComboBox<Stock> stockBox;
    private Label selectedStockPriceLabel;
    private Label selectedStockSectorLabel;
    private Label selectedHoldingInfoLabel;
    private String selectedSide = "BUY";
    private Button buySideBtn;
    private Button sellSideBtn;
    private ComboBox<String> typeBox;
    private TextField qtyField;
    private TextField priceField;
    private Label previewCostLabel;
    private Label previewFeeLabel;
    private Label previewTotalLabel;
    private Label msgLabel;
    private Button placeBtn;

    // Formatting
    private final NumberFormat currencyFmt = NumberFormat.getCurrencyInstance(Locale.US);
    private final NumberFormat percentFmt = NumberFormat.getPercentInstance(Locale.US);
    private final NumberFormat numberFmt = NumberFormat.getNumberInstance(Locale.US);

    // Theme (Dark default for professional financial desktop terminal)
    private boolean isDarkTheme = true;

    // Price tracking for change indicators
    private final Map<String, Double> previousPrices = new ConcurrentHashMap<>();

    // Live simulation
    private Timeline priceSimulationTimeline;

    public MainWindow(Database db) {
        this.db = db;
        numberFmt.setMinimumFractionDigits(0);
        numberFmt.setMaximumFractionDigits(0);
        percentFmt.setMinimumFractionDigits(2);
        percentFmt.setMaximumFractionDigits(2);
    }

    public void show(Stage s) {
        this.stage = s;
        login();
    }

    // ==================== LOGIN SCREEN ====================
    private void login() {
        StackPane loginRoot = new StackPane();
        loginRoot.getStyleClass().add("login-container");
        applyThemeClass(loginRoot);

        VBox card = new VBox(22);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Brand badge & Title
        Label badge = new Label("TRADESIM DESKTOP TERMINAL");
        badge.getStyleClass().add("login-badge");

        Label title = new Label("Sign in to your account");
        title.getStyleClass().add("login-title");

        Label subtitle = new Label("Execute real-time simulated equity orders and manage your portfolio.");
        subtitle.getStyleClass().add("login-subtitle");
        subtitle.setWrapText(true);

        VBox header = new VBox(8, badge, title, subtitle);

        // Form Fields
        VBox userGroup = new VBox(6);
        Label userLabel = new Label("USERNAME");
        userLabel.getStyleClass().add("field-label");
        TextField userField = new TextField("demo");
        userField.setPromptText("Enter username");
        userField.getStyleClass().add("input-field");
        userGroup.getChildren().addAll(userLabel, userField);

        VBox passGroup = new VBox(6);
        Label passLabel = new Label("PASSWORD");
        passLabel.getStyleClass().add("field-label");
        PasswordField passField = new PasswordField();
        passField.setText("demo123");
        passField.setPromptText("Enter password");
        passField.getStyleClass().add("input-field");
        passGroup.getChildren().addAll(passLabel, passField);

        VBox fields = new VBox(16, userGroup, passGroup);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("login-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        Button loginBtn = new Button("Launch Trading Terminal  →");
        loginBtn.getStyleClass().addAll("btn", "btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(42);
        loginBtn.setDefaultButton(true);

        loginBtn.setOnAction(e -> {
            String u = userField.getText().trim();
            String p = passField.getText();
            if (u.isEmpty() || p.isEmpty()) {
                showLoginError(errorLabel, "Please enter both username and password.");
                return;
            }
            Optional<User> opt = db.findUser(u, p);
            if (opt.isPresent()) {
                user = opt.get();
                main();
            } else {
                showLoginError(errorLabel, "Invalid credentials. Try demo / demo123.");
            }
        });

        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label footerHint = new Label("Demo Account: demo / demo123");
        footerHint.getStyleClass().add("text-muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button themeBtn = new Button(isDarkTheme ? "☀ Light" : "🌙 Dark");
        themeBtn.getStyleClass().addAll("btn", "btn-secondary");
        themeBtn.setStyle("-fx-padding: 4 10 4 10; -fx-font-size: 11px;");
        themeBtn.setOnAction(e -> {
            isDarkTheme = !isDarkTheme;
            themeBtn.setText(isDarkTheme ? "☀ Light" : "🌙 Dark");
            applyThemeClass(loginRoot);
        });
        footer.getChildren().addAll(footerHint, spacer, themeBtn);

        card.getChildren().addAll(header, fields, errorLabel, loginBtn, footer);
        loginRoot.getChildren().add(card);

        Scene scene = new Scene(loginRoot, 960, 660);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("TradeSim — Institutional Trading Simulator");
        stage.centerOnScreen();
        stage.show();

        Platform.runLater(userField::requestFocus);
    }

    private void showLoginError(Label errorLabel, String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    // ==================== MAIN TRADING WORKSPACE ====================
    private void main() {
        marketStocks.setAll(db.allStocks());
        for (Stock s : marketStocks) {
            previousPrices.put(s.symbol, s.price);
        }
        filteredMarketStocks = new FilteredList<>(marketStocks, p -> true);
        holdings.setAll(db.getHoldings(user.id));
        orders.setAll(db.getOrders(user.id));

        // Top section: Navigation header + Executive KPI bar
        VBox topSection = new VBox(createTopHeader(), createKpiBar());

        // Center Workspace: Split between Left Data Views and Right Order Ticket
        HBox workspace = new HBox(18);
        workspace.getStyleClass().add("workspace-pane");

        Node leftArea = createLeftDataArea();
        Node rightTicket = createOrderTicketPanel();

        HBox.setHgrow(leftArea, Priority.ALWAYS);
        workspace.getChildren().addAll(leftArea, rightTicket);

        mainLayout = new BorderPane();
        mainLayout.setTop(topSection);
        mainLayout.setCenter(workspace);

        rootOverlay = new StackPane(mainLayout);
        applyThemeClass(rootOverlay);

        Scene scene = new Scene(rootOverlay, 1280, 820);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("TradeSim — Trading Workstation (" + user.name + ")");
        stage.centerOnScreen();
        stage.show();

        if (!marketStocks.isEmpty()) {
            stockBox.setValue(marketStocks.get(0));
            marketTable.getSelectionModel().select(0);
        }

        updateAllMetrics();
        setupKeyboardShortcuts(scene);
        startPriceSimulation();
    }

    private HBox createTopHeader() {
        HBox header = new HBox(14);
        header.getStyleClass().add("top-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label logo = new Label("TS");
        logo.getStyleClass().add("brand-logo");

        Label title = new Label("TradeSim Workstation");
        title.getStyleClass().add("brand-title");

        Label liveBadge = new Label("● MARKET OPEN");
        liveBadge.getStyleClass().add("brand-tag");

        // Segmented view switcher in the header
        HBox navBar = new HBox(4);
        navBar.getStyleClass().add("nav-pill-bar");
        navBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(navBar, new Insets(0, 0, 0, 20));

        navMarketBtn = new Button("Market Watch");
        navPortfolioBtn = new Button("Portfolio Holdings");
        navOrdersBtn = new Button("Order Blotter");

        navMarketBtn.getStyleClass().addAll("nav-pill-btn", "active");
        navPortfolioBtn.getStyleClass().add("nav-pill-btn");
        navOrdersBtn.getStyleClass().add("nav-pill-btn");

        navMarketBtn.setOnAction(e -> switchLeftView(0));
        navPortfolioBtn.setOnAction(e -> switchLeftView(1));
        navOrdersBtn.setOnAction(e -> switchLeftView(2));

        navBar.getChildren().addAll(navMarketBtn, navPortfolioBtn, navOrdersBtn);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button simBtn = new Button("⚡ Step Market");
        simBtn.getStyleClass().addAll("btn", "btn-secondary");
        simBtn.setOnAction(e -> simulateMarket());

        Button refreshBtn = new Button("⟳ Refresh");
        refreshBtn.getStyleClass().addAll("btn", "btn-secondary");
        refreshBtn.setOnAction(e -> refreshAllData());

        Button exportBtn = new Button("⬇ Export CSV");
        exportBtn.getStyleClass().addAll("btn", "btn-secondary");
        exportBtn.setOnAction(e -> exportPortfolioCSV());

        Button themeBtn = new Button(isDarkTheme ? "☀ Light" : "🌙 Dark");
        themeBtn.getStyleClass().addAll("btn", "btn-secondary");
        themeBtn.setOnAction(e -> {
            isDarkTheme = !isDarkTheme;
            themeBtn.setText(isDarkTheme ? "☀ Light" : "🌙 Dark");
            applyThemeClass(rootOverlay);
        });

        Label userPill = new Label("👤 " + user.name.toUpperCase());
        userPill.getStyleClass().add("symbol-badge");

        header.getChildren().addAll(logo, title, liveBadge, navBar, spacer, simBtn, refreshBtn, exportBtn, themeBtn, userPill);
        return header;
    }

    private HBox createKpiBar() {
        HBox kpiBar = new HBox(16);
        kpiBar.getStyleClass().add("kpi-bar");
        kpiBar.setAlignment(Pos.CENTER_LEFT);

        // 1. Net Account Value
        VBox netWorthCard = new VBox(4);
        netWorthCard.getStyleClass().add("kpi-card");
        HBox.setHgrow(netWorthCard, Priority.ALWAYS);
        Label nwTitle = new Label("NET ACCOUNT VALUE");
        nwTitle.getStyleClass().add("kpi-label");
        netWorthValLabel = new Label("$0.00");
        netWorthValLabel.getStyleClass().add("kpi-value");
        Label nwSub = new Label("Cash + Market Equity");
        nwSub.getStyleClass().add("kpi-sub");
        netWorthCard.getChildren().addAll(nwTitle, netWorthValLabel, nwSub);

        // 2. Buying Power (Cash)
        VBox cashCard = new VBox(4);
        cashCard.getStyleClass().add("kpi-card");
        HBox.setHgrow(cashCard, Priority.ALWAYS);
        Label cashTitle = new Label("AVAILABLE CASH");
        cashTitle.getStyleClass().add("kpi-label");
        cashValLabel = new Label("$0.00");
        cashValLabel.getStyleClass().add("kpi-value");
        Label cashSub = new Label("Immediate Buying Power");
        cashSub.getStyleClass().add("kpi-sub");
        cashCard.getChildren().addAll(cashTitle, cashValLabel, cashSub);

        // 3. Holdings Equity
        VBox equityCard = new VBox(4);
        equityCard.getStyleClass().add("kpi-card");
        HBox.setHgrow(equityCard, Priority.ALWAYS);
        Label eqTitle = new Label("INVESTED EQUITY");
        eqTitle.getStyleClass().add("kpi-label");
        portfolioValLabel = new Label("$0.00");
        portfolioValLabel.getStyleClass().add("kpi-value");
        Label eqSub = new Label("Active Positions Value");
        eqSub.getStyleClass().add("kpi-sub");
        equityCard.getChildren().addAll(eqTitle, portfolioValLabel, eqSub);

        // 4. Unrealized P&L
        VBox pnlCard = new VBox(4);
        pnlCard.getStyleClass().add("kpi-card");
        HBox.setHgrow(pnlCard, Priority.ALWAYS);
        Label pnlTitle = new Label("UNREALIZED P&L");
        pnlTitle.getStyleClass().add("kpi-label");
        totalPnlValLabel = new Label("$0.00");
        totalPnlValLabel.getStyleClass().add("kpi-value");
        totalPnlSubLabel = new Label("Open Return on Cost");
        totalPnlSubLabel.getStyleClass().add("kpi-sub");
        pnlCard.getChildren().addAll(pnlTitle, totalPnlValLabel, totalPnlSubLabel);

        kpiBar.getChildren().addAll(netWorthCard, cashCard, equityCard, pnlCard);
        return kpiBar;
    }

    // ==================== LEFT DATA WORKSPACE ====================
    private Node createLeftDataArea() {
        leftContentStack = new StackPane();

        marketViewPane = createMarketWatchPane();
        portfolioViewPane = createPortfolioPane();
        ordersViewPane = createOrdersPane();

        portfolioViewPane.setVisible(false);
        ordersViewPane.setVisible(false);

        leftContentStack.getChildren().addAll(marketViewPane, portfolioViewPane, ordersViewPane);
        return leftContentStack;
    }

    private void switchLeftView(int index) {
        marketViewPane.setVisible(index == 0);
        portfolioViewPane.setVisible(index == 1);
        ordersViewPane.setVisible(index == 2);

        navMarketBtn.getStyleClass().remove("active");
        navPortfolioBtn.getStyleClass().remove("active");
        navOrdersBtn.getStyleClass().remove("active");

        if (index == 0) navMarketBtn.getStyleClass().add("active");
        else if (index == 1) navPortfolioBtn.getStyleClass().add("active");
        else if (index == 2) navOrdersBtn.getStyleClass().add("active");
    }

    private VBox createMarketWatchPane() {
        VBox card = new VBox(14);
        card.getStyleClass().add("panel-card");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleGroup = new VBox(2);
        Label title = new Label("Market Quotes");
        title.getStyleClass().add("panel-title");
        Label sub = new Label("Click any instrument to load it into the order ticket");
        sub.getStyleClass().add("panel-subtitle");
        titleGroup.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("Filter symbol, company, or sector...");
        searchField.getStyleClass().add("input-field");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            String q = newV == null ? "" : newV.trim().toLowerCase();
            filteredMarketStocks.setPredicate(s -> {
                if (q.isEmpty()) return true;
                return s.symbol.toLowerCase().contains(q)
                    || s.name.toLowerCase().contains(q)
                    || s.sector.toLowerCase().contains(q);
            });
        });

        header.getChildren().addAll(titleGroup, spacer, searchField);

        marketTable = createMarketTable();
        VBox.setVgrow(marketTable, Priority.ALWAYS);

        card.getChildren().addAll(header, marketTable);
        return card;
    }

    private TableView<Stock> createMarketTable() {
        TableView<Stock> table = new TableView<>(filteredMarketStocks);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No matching stocks found."));

        // Symbol Column with Badge
        TableColumn<Stock, String> symbolCol = new TableColumn<>("SYMBOL");
        symbolCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().symbol));
        symbolCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("symbol-badge");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        symbolCol.setMinWidth(95);
        symbolCol.setMaxWidth(115);

        // Company Name Column
        TableColumn<Stock, String> nameCol = createTableCol("INSTRUMENT", Stock::name, false);
        nameCol.setMinWidth(160);

        // Sector Badge Column
        TableColumn<Stock, String> sectorCol = new TableColumn<>("SECTOR");
        sectorCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().sector));
        sectorCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item.toUpperCase());
                    badge.getStyleClass().add("sector-badge");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        sectorCol.setMinWidth(100);
        sectorCol.setMaxWidth(130);

        // Last Price Column
        TableColumn<Stock, String> priceCol = createTableCol("LAST PRICE", s -> currencyFmt.format(s.price), true);
        priceCol.setMinWidth(110);

        // Session Change Pill Column
        TableColumn<Stock, String> changeCol = new TableColumn<>("CHANGE");
        changeCol.getStyleClass().add("numeric-column");
        changeCol.setCellValueFactory(c -> {
            Stock s = c.getValue();
            Double prev = previousPrices.get(s.symbol);
            if (prev != null && prev > 0) {
                double diff = s.price - prev;
                double pct = diff / prev;
                String sign = diff > 0.0001 ? "+" : "";
                return new SimpleStringProperty(sign + currencyFmt.format(diff) + " (" + sign + percentFmt.format(pct) + ")");
            }
            return new SimpleStringProperty("0.00 (0.00%)");
        });
        changeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label(item);
                    if (item.startsWith("+")) {
                        pill.getStyleClass().add("change-pill-up");
                    } else if (item.startsWith("-")) {
                        pill.getStyleClass().add("change-pill-down");
                    } else {
                        pill.getStyleClass().add("change-pill-neutral");
                    }
                    setGraphic(pill);
                    setAlignment(Pos.CENTER_RIGHT);
                    setText(null);
                }
            }
        });
        changeCol.setMinWidth(160);

        // Quick Trade Action Column
        TableColumn<Stock, String> actionCol = new TableColumn<>("QUICK TRADE");
        actionCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().symbol));
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button buyBtn = new Button("Buy");
            private final Button sellBtn = new Button("Sell");
            private final HBox box = new HBox(6, buyBtn, sellBtn);
            {
                box.setAlignment(Pos.CENTER_RIGHT);
                buyBtn.getStyleClass().add("quick-qty-btn");
                buyBtn.setStyle("-fx-text-fill: -color-success; -fx-border-color: -color-success;");
                sellBtn.getStyleClass().add("quick-qty-btn");
                sellBtn.setStyle("-fx-text-fill: -color-danger; -fx-border-color: -color-danger;");

                buyBtn.setOnAction(e -> {
                    Stock s = getTableView().getItems().get(getIndex());
                    loadStockIntoTicket(s, "BUY");
                });
                sellBtn.setOnAction(e -> {
                    Stock s = getTableView().getItems().get(getIndex());
                    loadStockIntoTicket(s, "SELL");
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        actionCol.setMinWidth(130);
        actionCol.setMaxWidth(150);

        table.getColumns().addAll(symbolCol, nameCol, sectorCol, priceCol, changeCol, actionCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                stockBox.setValue(newSel);
            }
        });

        return table;
    }

    private VBox createPortfolioPane() {
        VBox card = new VBox(14);
        card.getStyleClass().add("panel-card");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleGroup = new VBox(2);
        Label title = new Label("Portfolio Holdings");
        title.getStyleClass().add("panel-title");
        Label sub = new Label("Real-time valuation and unrealized profit/loss per position");
        sub.getStyleClass().add("panel-subtitle");
        titleGroup.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportBtn = new Button("⬇ Export Positions CSV");
        exportBtn.getStyleClass().addAll("btn", "btn-secondary");
        exportBtn.setOnAction(e -> exportPortfolioCSV());

        header.getChildren().addAll(titleGroup, spacer, exportBtn);

        portfolioTable = createHoldingsTable();
        VBox.setVgrow(portfolioTable, Priority.ALWAYS);

        card.getChildren().addAll(header, portfolioTable);
        return card;
    }

    private TableView<Holding> createHoldingsTable() {
        TableView<Holding> table = new TableView<>(holdings);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No active positions. Use the Order Ticket on the right to buy stocks."));

        TableColumn<Holding, String> symCol = new TableColumn<>("SYMBOL");
        symCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().symbol));
        symCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("symbol-badge");
                    setGraphic(badge);
                }
            }
        });
        symCol.setMaxWidth(110);

        TableColumn<Holding, String> nameCol = createTableCol("COMPANY", Holding::name, false);
        TableColumn<Holding, String> qtyCol = createTableCol("SHARES", h -> numberFmt.format(h.qty), true);
        TableColumn<Holding, String> avgCol = createTableCol("AVG COST", h -> currencyFmt.format(h.avgPrice), true);
        TableColumn<Holding, String> curCol = createTableCol("MARKET PRICE", h -> currencyFmt.format(h.curPrice), true);
        TableColumn<Holding, String> valCol = createTableCol("MARKET VALUE", h -> currencyFmt.format(h.value()), true);

        TableColumn<Holding, String> pnlCol = new TableColumn<>("UNREALIZED P&L");
        pnlCol.getStyleClass().add("numeric-column");
        pnlCol.setCellValueFactory(c -> {
            Holding h = c.getValue();
            double pnl = h.pnl();
            double costBasis = h.qty * h.avgPrice;
            double pct = costBasis > 0 ? pnl / costBasis : 0;
            String sign = pnl >= 0 ? "+" : "";
            return new SimpleStringProperty(sign + currencyFmt.format(pnl) + " (" + sign + percentFmt.format(pct) + ")");
        });
        pnlCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label pill = new Label(item);
                    pill.getStyleClass().add(item.startsWith("-") ? "change-pill-down" : "change-pill-up");
                    setGraphic(pill);
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });
        pnlCol.setMinWidth(165);

        table.getColumns().addAll(symCol, nameCol, qtyCol, avgCol, curCol, valCol, pnlCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                marketStocks.stream()
                    .filter(s -> s.symbol.equals(newV.symbol))
                    .findFirst()
                    .ifPresent(s -> loadStockIntoTicket(s, "SELL"));
            }
        });

        return table;
    }

    private VBox createOrdersPane() {
        VBox card = new VBox(14);
        card.getStyleClass().add("panel-card");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleGroup = new VBox(2);
        Label title = new Label("Order Execution Blotter");
        title.getStyleClass().add("panel-title");
        Label sub = new Label("Audit log of all submitted market and limit orders");
        sub.getStyleClass().add("panel-subtitle");
        titleGroup.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportBtn = new Button("⬇ Export Orders CSV");
        exportBtn.getStyleClass().addAll("btn", "btn-secondary");
        exportBtn.setOnAction(e -> exportOrdersCSV());

        header.getChildren().addAll(titleGroup, spacer, exportBtn);

        ordersTable = createOrdersTable();
        VBox.setVgrow(ordersTable, Priority.ALWAYS);

        card.getChildren().addAll(header, ordersTable);
        return card;
    }

    private TableView<Order> createOrdersTable() {
        TableView<Order> table = new TableView<>(orders);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No orders submitted yet."));

        TableColumn<Order, String> idCol = createTableCol("ORDER ID", o -> "#" + o.id, true);
        idCol.setMaxWidth(95);

        TableColumn<Order, String> symbolCol = createTableCol("SYMBOL", Order::symbol, false);
        symbolCol.setMaxWidth(110);

        TableColumn<Order, String> sideCol = createTableCol("SIDE", Order::side, false,
            o -> "BUY".equals(o.side) ? "label-success" : "label-danger");
        sideCol.setMaxWidth(90);

        TableColumn<Order, String> typeCol = createTableCol("TYPE", Order::type, false);
        TableColumn<Order, String> qtyCol = createTableCol("QTY", o -> numberFmt.format(o.qty), true);
        TableColumn<Order, String> priceCol = createTableCol("EXEC PRICE", o -> currencyFmt.format(o.price), true);
        TableColumn<Order, String> totalCol = createTableCol("NOTIONAL", o -> currencyFmt.format(o.qty * o.price), true);

        TableColumn<Order, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    String statusClass = switch (item) {
                        case "FILLED" -> "status-filled";
                        case "CANCELLED" -> "status-cancelled";
                        default -> "status-pending";
                    };
                    badge.getStyleClass().addAll("status-badge", statusClass);
                    setGraphic(badge);
                }
            }
        });

        table.getColumns().addAll(idCol, symbolCol, sideCol, typeCol, qtyCol, priceCol, totalCol, statusCol);
        return table;
    }

    // ==================== RIGHT DOCKED ORDER TICKET ====================
    private Node createOrderTicketPanel() {
        VBox ticket = new VBox(14);
        ticket.getStyleClass().add("panel-card");
        ticket.setPrefWidth(360);
        ticket.setMinWidth(340);
        ticket.setMaxWidth(380);

        VBox header = new VBox(2);
        Label title = new Label("Order Execution Ticket");
        title.getStyleClass().add("panel-title");
        Label subtitle = new Label("Direct market & limit order entry");
        subtitle.getStyleClass().add("panel-subtitle");
        header.getChildren().addAll(title, subtitle);

        // Buy / Sell Segmented Toggle
        buySideBtn = new Button("BUY / LONG");
        sellSideBtn = new Button("SELL / EXIT");
        buySideBtn.getStyleClass().addAll("side-toggle-buy", "active");
        sellSideBtn.getStyleClass().add("side-toggle-sell");
        buySideBtn.setMaxWidth(Double.MAX_VALUE);
        sellSideBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(buySideBtn, Priority.ALWAYS);
        HBox.setHgrow(sellSideBtn, Priority.ALWAYS);

        buySideBtn.setOnAction(e -> setOrderSide("BUY"));
        sellSideBtn.setOnAction(e -> setOrderSide("SELL"));

        HBox sideToggleBox = new HBox(0, buySideBtn, sellSideBtn);

        // Instrument Selector
        VBox symGroup = new VBox(6);
        Label symLabel = new Label("INSTRUMENT");
        symLabel.getStyleClass().add("field-label");
        stockBox = new ComboBox<>(marketStocks);
        stockBox.setMaxWidth(Double.MAX_VALUE);
        stockBox.setPromptText("Select instrument...");
        stockBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Stock s) {
                return s != null ? s.symbol + " — " + s.name + " (" + currencyFmt.format(s.price) + ")" : "";
            }
            @Override
            public Stock fromString(String string) { return null; }
        });
        symGroup.getChildren().addAll(symLabel, stockBox);

        // Selected Stock Quote Banner
        VBox quoteBanner = new VBox(4);
        quoteBanner.getStyleClass().add("ticket-stock-banner");
        HBox quoteTop = new HBox(8);
        quoteTop.setAlignment(Pos.CENTER_LEFT);
        selectedStockPriceLabel = new Label("$0.00");
        selectedStockPriceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: 'Consolas';");
        Region qSpacer = new Region();
        HBox.setHgrow(qSpacer, Priority.ALWAYS);
        selectedStockSectorLabel = new Label("—");
        selectedStockSectorLabel.getStyleClass().add("sector-badge");
        quoteTop.getChildren().addAll(selectedStockPriceLabel, qSpacer, selectedStockSectorLabel);

        selectedHoldingInfoLabel = new Label("Current Position: 0 shares");
        selectedHoldingInfoLabel.getStyleClass().add("text-muted");
        quoteBanner.getChildren().addAll(quoteTop, selectedHoldingInfoLabel);

        // Order Type & Limit Price Row
        HBox typePriceRow = new HBox(10);
        VBox typeGroup = new VBox(6);
        Label typeLabel = new Label("ORDER TYPE");
        typeLabel.getStyleClass().add("field-label");
        typeBox = new ComboBox<>(FXCollections.observableArrayList("MARKET", "LIMIT"));
        typeBox.setValue("MARKET");
        typeBox.setMaxWidth(Double.MAX_VALUE);
        typeGroup.getChildren().addAll(typeLabel, typeBox);
        HBox.setHgrow(typeGroup, Priority.ALWAYS);

        VBox limitGroup = new VBox(6);
        Label priceLabel = new Label("LIMIT PRICE ($)");
        priceLabel.getStyleClass().add("field-label");
        priceField = new TextField();
        priceField.setPromptText("Market");
        priceField.setDisable(true);
        limitGroup.getChildren().addAll(priceLabel, priceField);
        HBox.setHgrow(limitGroup, Priority.ALWAYS);

        typePriceRow.getChildren().addAll(typeGroup, limitGroup);

        typeBox.valueProperty().addListener((obs, oldV, newV) -> {
            boolean isLimit = "LIMIT".equals(newV);
            priceField.setDisable(!isLimit);
            if (!isLimit) {
                priceField.clear();
            } else if (stockBox.getValue() != null) {
                priceField.setText(String.format(Locale.US, "%.2f", stockBox.getValue().price));
            }
            updateTradePreview();
        });

        // Quantity & Quick Preset Buttons
        VBox qtyGroup = new VBox(6);
        Label qtyLabel = new Label("SHARES QUANTITY");
        qtyLabel.getStyleClass().add("field-label");
        qtyField = new TextField("10");
        qtyField.setPromptText("Enter share quantity");

        HBox presetBox = new HBox(6);
        for (int preset : new int[]{1, 5, 10, 25, 50}) {
            Button b = new Button("+" + preset);
            b.getStyleClass().add("quick-qty-btn");
            b.setOnAction(e -> {
                int cur = 0;
                try { cur = Integer.parseInt(qtyField.getText().trim()); } catch (Exception ignored) {}
                qtyField.setText(String.valueOf(Math.max(0, cur) + preset));
            });
            presetBox.getChildren().add(b);
        }
        Button maxBtn = new Button("MAX");
        maxBtn.getStyleClass().add("quick-qty-btn");
        maxBtn.setOnAction(e -> setMaxQuantity());
        presetBox.getChildren().add(maxBtn);

        qtyGroup.getChildren().addAll(qtyLabel, qtyField, presetBox);

        // Order Summary Preview Box
        VBox previewBox = new VBox(8);
        previewBox.getStyleClass().add("trade-preview");

        previewCostLabel = new Label("$0.00");
        previewCostLabel.getStyleClass().add("trade-preview-value");
        previewFeeLabel = new Label("$0.00");
        previewFeeLabel.getStyleClass().add("trade-preview-value");
        previewTotalLabel = new Label("$0.00");
        previewTotalLabel.getStyleClass().add("trade-preview-total");

        previewBox.getChildren().addAll(
            createPreviewRow("Estimated Value", previewCostLabel),
            createPreviewRow("Commission (0.1%)", previewFeeLabel),
            new Separator(),
            createPreviewRow("Total Settlement", previewTotalLabel)
        );

        // Listeners for real-time preview update
        stockBox.valueProperty().addListener((o, a, b) -> updateTradePreview());
        qtyField.textProperty().addListener((o, a, b) -> updateTradePreview());
        priceField.textProperty().addListener((o, a, b) -> updateTradePreview());

        // Submit Button
        placeBtn = new Button("SUBMIT BUY ORDER");
        placeBtn.getStyleClass().addAll("btn", "btn-buy");
        placeBtn.setMaxWidth(Double.MAX_VALUE);
        placeBtn.setPrefHeight(44);
        placeBtn.setOnAction(e -> placeOrder());

        msgLabel = new Label();
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-font-size: 12px;");

        ticket.getChildren().addAll(
            header,
            sideToggleBox,
            symGroup,
            quoteBanner,
            typePriceRow,
            qtyGroup,
            previewBox,
            placeBtn,
            msgLabel
        );
        return ticket;
    }

    private HBox createPreviewRow(String labelText, Label valueLabel) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("trade-preview-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(lbl, spacer, valueLabel);
        return row;
    }

    private void setOrderSide(String side) {
        this.selectedSide = side;
        buySideBtn.getStyleClass().remove("active");
        sellSideBtn.getStyleClass().remove("active");
        placeBtn.getStyleClass().removeAll("btn-buy", "btn-sell");

        if ("BUY".equals(side)) {
            buySideBtn.getStyleClass().add("active");
            placeBtn.getStyleClass().add("btn-buy");
            placeBtn.setText("SUBMIT BUY ORDER");
        } else {
            sellSideBtn.getStyleClass().add("active");
            placeBtn.getStyleClass().add("btn-sell");
            placeBtn.setText("SUBMIT SELL ORDER");
        }
        updateTradePreview();
    }

    private void loadStockIntoTicket(Stock stock, String side) {
        if (stock == null) return;
        stockBox.setValue(stock);
        setOrderSide(side);
        qtyField.requestFocus();
    }

    private void setMaxQuantity() {
        Stock s = stockBox.getValue();
        if (s == null) return;
        if ("BUY".equals(selectedSide)) {
            double price = getEffectiveOrderPrice(s);
            if (price > 0) {
                int maxShares = (int) Math.floor(user.cash / (price * 1.001));
                qtyField.setText(String.valueOf(Math.max(1, maxShares)));
            }
        } else {
            int owned = holdings.stream()
                .filter(h -> h.symbol.equals(s.symbol))
                .mapToInt(h -> h.qty)
                .findFirst()
                .orElse(0);
            qtyField.setText(String.valueOf(Math.max(1, owned)));
        }
    }

    private double getEffectiveOrderPrice(Stock s) {
        if ("LIMIT".equals(typeBox.getValue())) {
            try {
                return Double.parseDouble(priceField.getText().trim());
            } catch (Exception e) {
                return 0;
            }
        }
        return s.price;
    }

    private void updateTradePreview() {
        Stock s = stockBox.getValue();
        if (s == null) {
            selectedStockPriceLabel.setText("$0.00");
            selectedStockSectorLabel.setText("—");
            selectedHoldingInfoLabel.setText("Current Position: 0 shares");
            previewCostLabel.setText("$0.00");
            previewFeeLabel.setText("$0.00");
            previewTotalLabel.setText("$0.00");
            return;
        }

        selectedStockPriceLabel.setText(currencyFmt.format(s.price));
        selectedStockSectorLabel.setText(s.sector.toUpperCase());

        Holding existing = holdings.stream().filter(h -> h.symbol.equals(s.symbol)).findFirst().orElse(null);
        if (existing != null) {
            selectedHoldingInfoLabel.setText("Position: " + existing.qty + " sh @ " + currencyFmt.format(existing.avgPrice));
        } else {
            selectedHoldingInfoLabel.setText("Current Position: 0 shares");
        }

        try {
            int qty = Integer.parseInt(qtyField.getText().trim());
            double price = getEffectiveOrderPrice(s);
            if (qty <= 0 || price <= 0) {
                previewCostLabel.setText("$0.00");
                previewFeeLabel.setText("$0.00");
                previewTotalLabel.setText("$0.00");
                return;
            }

            double cost = qty * price;
            double fee = cost * 0.001;
            double total = "BUY".equals(selectedSide) ? (cost + fee) : (cost - fee);

            previewCostLabel.setText(currencyFmt.format(cost));
            previewFeeLabel.setText(currencyFmt.format(fee));
            previewTotalLabel.setText(currencyFmt.format(total));
        } catch (NumberFormatException ex) {
            previewCostLabel.setText("$0.00");
            previewFeeLabel.setText("$0.00");
            previewTotalLabel.setText("$0.00");
        }
    }

    private void placeOrder() {
        try {
            Stock s = stockBox.getValue();
            if (s == null) {
                showTicketMsg("Please select an instrument.", true);
                return;
            }

            int qty = Integer.parseInt(qtyField.getText().trim());
            if (qty <= 0) {
                showTicketMsg("Quantity must be greater than zero.", true);
                return;
            }

            String type = typeBox.getValue();
            double price = getEffectiveOrderPrice(s);
            if (price <= 0) {
                showTicketMsg("Enter a valid limit price.", true);
                return;
            }

            double cost = qty * price;
            double fee = cost * 0.001;
            double total = cost + fee;

            if ("BUY".equals(selectedSide) && total > user.cash) {
                showTicketMsg("Insufficient buying power: need " + currencyFmt.format(total), true);
                return;
            }

            Holding existing = holdings.stream().filter(h -> h.symbol.equals(s.symbol)).findFirst().orElse(null);
            if ("SELL".equals(selectedSide)) {
                if (existing == null || existing.qty < qty) {
                    int owned = existing != null ? existing.qty : 0;
                    showTicketMsg("Insufficient shares to sell (Owned: " + owned + ").", true);
                    return;
                }
            }

            int id = db.placeOrder(user.id, s.symbol, selectedSide, type, qty, price);
            if ("MARKET".equals(type)) {
                executeOrderFill(id, s, qty, price, selectedSide, existing);
            }

            showTicketMsg("✓ " + selectedSide + " " + qty + " " + s.symbol + " @ " + currencyFmt.format(price), false);
            refreshAllData();
            showToast("Order Executed: " + selectedSide + " " + qty + " " + s.symbol, false);
        } catch (NumberFormatException ex) {
            showTicketMsg("Please enter a valid numeric quantity.", true);
        } catch (Exception ex) {
            showTicketMsg("Order error: " + ex.getMessage(), true);
        }
    }

    private void executeOrderFill(int orderId, Stock s, int qty, double price, String side, Holding existing) {
        double fee = qty * price * 0.001;
        if ("BUY".equals(side)) {
            user.cash -= (qty * price + fee);
            int newQty = (existing != null ? existing.qty : 0) + qty;
            double prevCost = (existing != null ? existing.qty * existing.avgPrice : 0);
            double newAvg = (prevCost + (qty * price)) / newQty;
            db.upsertHolding(user.id, s.symbol, newQty, newAvg);
        } else {
            user.cash += (qty * price - fee);
            int remaining = (existing != null ? existing.qty : 0) - qty;
            if (remaining > 0 && existing != null) {
                db.upsertHolding(user.id, s.symbol, remaining, existing.avgPrice);
            } else {
                db.deleteHolding(user.id, s.symbol);
            }
        }
        db.updateCash(user.id, user.cash);
        db.fillOrder(orderId, "FILLED");
    }

    private void showTicketMsg(String msg, boolean isError) {
        msgLabel.setText(msg);
        msgLabel.getStyleClass().removeAll("label-success", "label-danger");
        msgLabel.getStyleClass().add(isError ? "label-danger" : "label-success");
    }

    // ==================== DATA REFRESH & SIMULATION ====================
    private void updateAllMetrics() {
        double equity = holdings.stream().mapToDouble(Holding::value).sum();
        double pnl = holdings.stream().mapToDouble(Holding::pnl).sum();
        double costBasis = holdings.stream().mapToDouble(h -> h.qty * h.avgPrice).sum();
        double pnlPct = costBasis > 0 ? (pnl / costBasis) : 0;
        double netWorth = user.cash + equity;

        if (netWorthValLabel != null) netWorthValLabel.setText(currencyFmt.format(netWorth));
        if (cashValLabel != null) cashValLabel.setText(currencyFmt.format(user.cash));
        if (portfolioValLabel != null) portfolioValLabel.setText(currencyFmt.format(equity));
        if (totalPnlValLabel != null) {
            String sign = pnl >= 0 ? "+" : "";
            totalPnlValLabel.setText(sign + currencyFmt.format(pnl));
            totalPnlValLabel.getStyleClass().removeAll("price-up", "price-down");
            totalPnlValLabel.getStyleClass().add(pnl >= 0 ? "price-up" : "price-down");
        }
        if (totalPnlSubLabel != null) {
            String sign = pnl >= 0 ? "+" : "";
            totalPnlSubLabel.setText(sign + percentFmt.format(pnlPct) + " Total Return");
        }
    }

    private void refreshMarketData() {
        Stock currentlySelected = stockBox != null ? stockBox.getValue() : null;
        String selectedSym = currentlySelected != null ? currentlySelected.symbol : null;

        List<Stock> freshStocks = db.allStocks();
        for (Stock s : marketStocks) {
            previousPrices.put(s.symbol, s.price);
        }
        marketStocks.setAll(freshStocks);
        if (marketTable != null) marketTable.refresh();

        if (selectedSym != null && stockBox != null) {
            marketStocks.stream()
                .filter(s -> s.symbol.equals(selectedSym))
                .findFirst()
                .ifPresent(s -> stockBox.setValue(s));
        }

        holdings.setAll(db.getHoldings(user.id));
        if (portfolioTable != null) portfolioTable.refresh();
        updateAllMetrics();
        updateTradePreview();
    }

    private void refreshAllData() {
        refreshMarketData();
        orders.setAll(db.getOrders(user.id));
        if (ordersTable != null) ordersTable.refresh();
    }

    private void simulateMarket() {
        for (Stock s : marketStocks) {
            previousPrices.put(s.symbol, s.price);
            double change = s.price * (Math.random() * 0.05 - 0.025); // ±2.5%
            double newPrice = Math.max(1.0, Math.round((s.price + change) * 100.0) / 100.0);
            db.updatePrice(s.symbol, newPrice);
        }
        refreshMarketData();
    }

    private void startPriceSimulation() {
        if (priceSimulationTimeline != null) {
            priceSimulationTimeline.stop();
        }
        priceSimulationTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (stage != null && stage.isShowing()) {
                for (Stock s : marketStocks) {
                    previousPrices.put(s.symbol, s.price);
                    double change = s.price * (Math.random() * 0.018 - 0.009);
                    double newPrice = Math.max(1.0, Math.round((s.price + change) * 100.0) / 100.0);
                    db.updatePrice(s.symbol, newPrice);
                }
                Platform.runLater(this::refreshMarketData);
            }
        }));
        priceSimulationTimeline.setCycleCount(Animation.INDEFINITE);
        priceSimulationTimeline.play();
    }

    // ==================== HELPERS & SHORTCUTS ====================
    private void applyThemeClass(Pane root) {
        if (root == null) return;
        root.getStyleClass().removeAll("light", "dark");
        root.getStyleClass().add(isDarkTheme ? "dark" : "light");
    }

    private void setupKeyboardShortcuts(Scene scene) {
        scene.getAccelerators().put(KeyCombination.keyCombination("Ctrl+1"), () -> switchLeftView(0));
        scene.getAccelerators().put(KeyCombination.keyCombination("Ctrl+2"), () -> switchLeftView(1));
        scene.getAccelerators().put(KeyCombination.keyCombination("Ctrl+3"), () -> switchLeftView(2));
        scene.getAccelerators().put(KeyCombination.keyCombination("Ctrl+R"), this::refreshAllData);
    }

    private <T> TableColumn<T, String> createTableCol(String title, Function<T, String> mapper, boolean numeric) {
        return createTableCol(title, mapper, numeric, null);
    }

    private <T> TableColumn<T, String> createTableCol(String title, Function<T, String> mapper, boolean numeric, Function<T, String> rowStyle) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cellData -> new SimpleStringProperty(mapper.apply(cellData.getValue())));
        if (numeric) col.getStyleClass().add("numeric-column");
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("label-success", "label-danger", "holding-positive", "holding-negative", "numeric-column");
                if (numeric) getStyleClass().add("numeric-column");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    if (rowStyle != null && getTableRow() != null && getTableRow().getItem() != null) {
                        String style = rowStyle.apply(getTableRow().getItem());
                        if (style != null && !style.isEmpty()) {
                            getStyleClass().add(style);
                        }
                    }
                }
            }
        });
        return col;
    }

    private void exportPortfolioCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Portfolio to CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("portfolio-" + java.time.LocalDate.now() + ".csv");
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Symbol,Company,Quantity,Avg Price,Current Price,Market Value,P&L");
            for (Holding h : holdings) {
                writer.printf(Locale.US, "%s,%s,%d,%.2f,%.2f,%.2f,%.2f%n",
                    h.symbol, h.name.replace(",", ""), h.qty, h.avgPrice, h.curPrice, h.value(), h.pnl());
            }
            showToast("Portfolio exported to " + file.getName(), false);
        } catch (Exception ex) {
            showToast("Export failed: " + ex.getMessage(), true);
        }
    }

    private void exportOrdersCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Orders to CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("orders-" + java.time.LocalDate.now() + ".csv");
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Order ID,Symbol,Side,Type,Quantity,Price,Status");
            for (Order o : orders) {
                writer.printf(Locale.US, "%d,%s,%s,%s,%d,%.2f,%s%n",
                    o.id, o.symbol, o.side, o.type, o.qty, o.price, o.status);
            }
            showToast("Orders exported to " + file.getName(), false);
        } catch (Exception ex) {
            showToast("Export failed: " + ex.getMessage(), true);
        }
    }

    private void showToast(String message, boolean isError) {
        if (rootOverlay == null) return;
        Platform.runLater(() -> {
            HBox toast = new HBox(10);
            toast.getStyleClass().addAll("toast", isError ? "toast-error" : "toast-success");
            toast.setAlignment(Pos.CENTER_LEFT);
            toast.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

            Label lbl = new Label(message);
            lbl.setStyle("-fx-font-weight: bold;");
            toast.getChildren().add(lbl);

            StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(toast, new Insets(0, 28, 28, 0));
            rootOverlay.getChildren().add(toast);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(180), toast);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), toast);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev -> rootOverlay.getChildren().remove(toast));
                fadeOut.play();
            });
            delay.play();
        });
    }
}
