package com.tradesim.tradesim;

import com.tradesim.tradesim.db.Database;
import com.tradesim.tradesim.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * JavaFX Application with Spring Boot Integration.
 * Runs both Spring Boot REST API and JavaFX Desktop UI.
 */
public class TradeSimApp extends Application {

    private ConfigurableApplicationContext springContext;
    private Database database;

    @Override
    public void init() {
        // Start Spring Boot context
        springContext = new SpringApplicationBuilder(SpringBootApp.class).run();
        database = springContext.getBean(Database.class);
    }

    @Override
    public void start(Stage stage) {
        new MainWindow(database).show(stage);
    }

    @Override
    public void stop() {
        springContext.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}