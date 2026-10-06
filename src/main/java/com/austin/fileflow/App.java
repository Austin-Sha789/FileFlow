package com.austin.fileflow;

import javafx.application.Application;
import javafx.stage.Stage;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;


public class App extends Application
{
    public static void main( String[] args )
    {
        launch(args);
    }

    @Override 
    public void start (Stage stage)
    {
        BorderPane root = new BorderPane();

        VBox sidebar = new VBox();
        sidebar.setPrefWidth(220);
        sidebar.getStyleClass().add("sidebar");

        Label logo = new Label("FileFlow");

        Label dashboard = new Label("Dashboard");
        Label files = new Label("Files");
        Label favorites = new Label("Favorites");
        Label duplicates = new Label("Duplicates");
        Label tags = new Label("Tags");
        Label history = new Label("History");
        Label settings = new Label("Settings");

        sidebar.getChildren().add(logo);
        sidebar.getChildren().add(dashboard);
        sidebar.getChildren().add(files);
        sidebar.getChildren().add(favorites);
        sidebar.getChildren().add(duplicates);
        sidebar.getChildren().add(tags);
        sidebar.getChildren().add(history);
        sidebar.getChildren().add(settings);

        sidebar.setSpacing(18);

        dashboard.getStyleClass().add("nav-item");
        files.getStyleClass().add("nav-item");
        favorites.getStyleClass().add("nav-item");
        duplicates.getStyleClass().add("nav-item");
        tags.getStyleClass().add("nav-item");
        history.getStyleClass().add("nav-item");
        settings.getStyleClass().add("nav-item");

        Label content = new Label("Dashboard");
        content.getStyleClass().add("page-title");

        root.setLeft(sidebar);
        root.setCenter(content);

        Scene scene = new Scene(root, 1200, 760);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        stage.setTitle("FileFlow");
        stage.setScene(scene);
        stage.show();
    }
}
