package com.austin.fileflow.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

/** Keeps the original full-resolution artwork available to HiDPI rendering. */
public final class AppIcon {
    private static final String URL = AppIcon.class.getResource("/icons/fileflow.png").toExternalForm();
    private static final Image ORIGINAL = new Image(URL, false);

    private AppIcon() { }

    public static ImageView view(double logicalSize) {
        ImageView view = new ImageView(ORIGINAL);
        view.setFitWidth(logicalSize);
        view.setFitHeight(logicalSize);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setCache(false);
        return view;
    }

    public static void install(Stage stage) {
        stage.getIcons().clear();
        for (int size : new int[] {16, 24, 32, 48, 64, 128, 256}) {
            stage.getIcons().add(new Image(URL, size, size, true, true, false));
        }
        stage.getIcons().add(ORIGINAL);
    }
}
