package com.austin.fileflow;

import javafx.application.Application;
import javafx.stage.Stage;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import com.austin.fileflow.ui.MainView;


public class App extends Application
{
    public static void main( String[] args )
    {
        launch(args);
    }

    @Override 
    public void start (Stage stage)
    {
        MainView mainView = new MainView();

        Scene scene = new Scene(mainView.getView(), 1200, 760);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        stage.setTitle("FileFlow");
        stage.setScene(scene);
        stage.show();
    }
}
