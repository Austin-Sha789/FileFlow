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

    /** Stage icons do not control the process-wide macOS Dock icon. */
    public static void installDockIcon() {
        if (!System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("mac")) return;
        java.awt.EventQueue.invokeLater(() -> {
            try {
                if (!java.awt.Taskbar.isTaskbarSupported()) return;
                var taskbar = java.awt.Taskbar.getTaskbar();
                if (!taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) return;
                try (var stream = AppIcon.class.getResourceAsStream("/icons/fileflow.png")) {
                    var image = javax.imageio.ImageIO.read(stream);
                    if (image != null) taskbar.setIconImage(image);
                }
            } catch (java.io.IOException | UnsupportedOperationException | SecurityException error) {
                // Some launchers do not expose a Dock; never prevent the window from opening.
                System.getLogger(AppIcon.class.getName()).log(System.Logger.Level.DEBUG, "Dock icon unavailable", error);
            }
        });
    }

    public static void install(Stage stage) {
        stage.getIcons().clear();
        for (int size : new int[] {16, 24, 32, 48, 64, 128, 256}) {
            stage.getIcons().add(new Image(URL, size, size, true, true, false));
        }
        stage.getIcons().add(ORIGINAL);
    }
}
