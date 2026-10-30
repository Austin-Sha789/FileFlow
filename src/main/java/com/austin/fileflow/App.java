package com.austin.fileflow;

import javafx.application.Application;
import javafx.stage.Stage;

import javafx.scene.Scene;

import com.austin.fileflow.database.DatabaseManager;
import com.austin.fileflow.repository.FavouriteRepo;
import com.austin.fileflow.repository.ScanHistoryRepo;
import com.austin.fileflow.repository.TagRepo;
import com.austin.fileflow.ui.MainView;
import com.austin.fileflow.ui.AppIcon;
import com.austin.fileflow.ui.OceanWindowChrome;
import com.austin.fileflow.platform.FilePlatformService;
import com.austin.fileflow.service.ShortcutManager;
import com.austin.fileflow.service.FileScanService;
import com.austin.fileflow.service.DuplicateService;
import com.austin.fileflow.service.StorageService;
import com.austin.fileflow.ui.component.FilePreviewFactory;

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
        TagRepo tagRepo = new TagRepo(databaseManager);
        ScanHistoryRepo scanHistoryRepo = new ScanHistoryRepo(databaseManager);
        ShortcutManager shortcutManager = new ShortcutManager();
        FilePlatformService filePlatformService = new FilePlatformService();
        FileScanService fileScanService = new FileScanService();
        DuplicateService duplicateService = new DuplicateService();
        StorageService storageService = new StorageService();
        FilePreviewFactory filePreviewFactory = new FilePreviewFactory();

        MainView mainView = new MainView(
            favouriteRepo, 
            tagRepo, 
            scanHistoryRepo, 
            shortcutManager, 
            filePlatformService,
            fileScanService,
            duplicateService,
            storageService,
            filePreviewFactory
        );

        Scene scene = new Scene(mainView.getView(), 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        scene.getStylesheets().add(getClass().getResource("/css/ocean.css").toExternalForm());
        stage.setMinWidth(1060);
        stage.setMinHeight(680);
        AppIcon.install(stage);
        stage.setTitle("FileFlow");
        OceanWindowChrome.install(stage, scene);
        stage.setScene(scene);
        stage.show();
        AppIcon.installDockIcon();
    }
}
