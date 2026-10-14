package com.austin.fileflow;

import javafx.application.Application;
import javafx.stage.Stage;

import javafx.scene.Scene;

import com.austin.fileflow.database.DatabaseManager;
import com.austin.fileflow.repository.FavouriteRepo;
import com.austin.fileflow.ui.MainView;


public class App extends Application
{
    public static void main( String[] args )
    {
        launch(args);
    }

    @Override 
    public void start (Stage stage) throws Exception
    {
        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.initialiseDatabase();

        FavouriteRepo favouriteRepo = new FavouriteRepo(databaseManager);

        MainView mainView = new MainView(favouriteRepo);

        Scene scene = new Scene(mainView.getView(), 1200, 760);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        stage.setTitle("FileFlow");
        stage.setScene(scene);
        stage.show();
    }
}
