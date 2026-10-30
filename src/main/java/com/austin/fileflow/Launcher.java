package com.austin.fileflow;

import java.nio.file.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import com.austin.fileflow.database.DatabaseManager;
import com.austin.fileflow.update.BuildInfo;

/** A plain entry point lets the bundled JavaFX jars load from the packaged class path. */
public final class Launcher {
    private Launcher() { }
    public static void main(String[] args) {
        if (args.length == 2 && "--self-test".equals(args[0])) Application.launch(PackageSmokeTest.class, args[1]);
        else App.main(args);
    }
    public static final class PackageSmokeTest extends Application {
        @Override public void start(Stage stage) throws Exception {
            Path report = Path.of(getParameters().getRaw().get(0)).toAbsolutePath();
            Files.createDirectories(report.getParent());
            System.setProperty("fileflow.dataDir", report.getParent().resolve("smoke-user-data").toString());
            new App().start(stage);
            Platform.runLater(() -> {
                try {
                    stage.getScene().getRoot().applyCss(); stage.getScene().getRoot().layout();
                    try (var connection = new DatabaseManager().getConnection(); var statement = connection.createStatement();
                         var result = statement.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='favourites'")) {
                        if (!result.next() || result.getInt(1) != 1) throw new IllegalStateException("Database smoke test failed");
                    }
                    var result = new com.google.gson.JsonObject();
                    result.addProperty("version", BuildInfo.VERSION); result.addProperty("os", System.getProperty("os.name"));
                    result.addProperty("arch", System.getProperty("os.arch")); result.addProperty("stageStyle", stage.getStyle().name());
                    result.addProperty("javafx", true); result.addProperty("sqlite", true);
                    Files.writeString(report, result.toString()); stage.close(); Platform.exit();
                } catch (Exception error) { error.printStackTrace(); System.exit(1); }
            });
        }
    }
}
