package com.austin.fileflow.ui;

import static com.austin.fileflow.i18n.I18n.tr;

import javafx.css.PseudoClass;
import javafx.application.Platform;
import javafx.application.ConditionalFeature;
import java.util.Locale;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/** Native macOS decorations; dark draggable caption controls on other desktops. */
public final class OceanWindowChrome {
    private static final double EDGE = 6;
    private OceanWindowChrome() { }

    public static void install(Stage stage, Scene scene) {
        install(stage, scene, isMac());
    }

    public static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    // Platform policy is injectable so both layouts can be verified without spoofing os.name.
    static void install(Stage stage, Scene scene, boolean mac) {
        Node top = scene.getRoot().lookup(".ff-command-bar");
        if (!(top instanceof HBox bar)) throw new IllegalStateException("FileFlow command bar is missing");
        if (mac) {
            stage.initStyle(Platform.isSupported(ConditionalFeature.UNIFIED_WINDOW) ? StageStyle.UNIFIED : StageStyle.DECORATED);
            scene.setFill(Color.TRANSPARENT);
            scene.getRoot().getStyleClass().add("ocean-platform-mac");
            Node brand = bar.lookup(".ocean-brand-box");
            if (brand != null && bar.getChildren().remove(brand)) {
                if (brand instanceof HBox box) box.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                bar.getChildren().add(brand);
            }
            // Cocoa owns traffic lights, zoom/full screen, dragging and resizing.
            // Do not attach Windows caption buttons or competing resize filters.
            return;
        }
        stage.initStyle(StageStyle.UNDECORATED);
        scene.setFill(Color.web("#070c15"));
        Button minimize = control(tr("ui.minimize"), "M3 8 H15", () -> stage.setIconified(true));
        Button maximize = control(tr("ui.maximize"), "M3 3 H15 V15 H3 Z", () -> stage.setMaximized(!stage.isMaximized()));
        Button close = control(tr("common.close"), "M4 4 L14 14 M14 4 L4 14", stage::close);
        close.getStyleClass().add("ocean-window-close");
        HBox controls = new HBox(0, minimize, maximize, close);
        controls.getStyleClass().add("ocean-window-controls");
        bar.getChildren().add(controls);
        stage.maximizedProperty().addListener((observable, before, maximized) -> {
            maximize.setAccessibleText(maximized ? tr("ui.restore_window") : tr("ui.maximize"));
            maximize.setTooltip(new Tooltip(maximized ? tr("ui.restore_window") : tr("ui.maximize")));
            ((SVGPath) maximize.getGraphic()).setContent(maximized
                    ? "M5 5 H14 V14 H5 Z M8 5 V2 H17 V11 H14" : "M3 3 H15 V15 H3 Z");
            scene.getRoot().pseudoClassStateChanged(PseudoClass.getPseudoClass("maximized"), maximized);
        });

        final double[] drag = new double[4];
        bar.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || interactive(event.getTarget())) return;
            drag[0] = event.getScreenX() - stage.getX();
            drag[1] = event.getScreenY() - stage.getY();
            drag[2] = Math.max(0, Math.min(1, event.getSceneX() / stage.getWidth()));
            drag[3] = 1;
        });
        bar.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!event.isPrimaryButtonDown() || drag[3] == 0 || interactive(event.getTarget())) return;
            if (stage.isMaximized()) {
                stage.setMaximized(false);
                drag[0] = stage.getWidth() * drag[2];
                drag[1] = Math.min(drag[1], 30);
            }
            stage.setX(event.getScreenX() - drag[0]);
            stage.setY(event.getScreenY() - drag[1]);
        });
        bar.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> drag[3] = 0);
        bar.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !interactive(event.getTarget())) {
                stage.setMaximized(!stage.isMaximized());
            }
        });
        com.austin.fileflow.i18n.I18n.instance().languageProperty().addListener((observable, before, after) -> {
            minimize.setAccessibleText(tr("ui.minimize")); minimize.setTooltip(new Tooltip(tr("ui.minimize")));
            String maximizeText = stage.isMaximized() ? tr("ui.restore_window") : tr("ui.maximize");
            maximize.setAccessibleText(maximizeText); maximize.setTooltip(new Tooltip(maximizeText));
            close.setAccessibleText(tr("common.close")); close.setTooltip(new Tooltip(tr("common.close")));
        });
        stage.iconifiedProperty().addListener((observable, before, after) -> resetControls(controls));
        stage.focusedProperty().addListener((observable, before, after) -> resetControls(controls));
        installResize(stage, scene);
    }

    private static void resetControls(HBox controls) {
        for (Node node : controls.getChildren()) if (node instanceof CaptionButton button) button.resetPointerState();
    }

    private static final class CaptionButton extends Button {
        void resetPointerState() { disarm(); setHover(false); setPressed(false); }
    }

    private static Button control(String accessibleName, String path, Runnable action) {
        SVGPath icon = new SVGPath();
        icon.setContent(path);
        icon.getStyleClass().add("ocean-caption-icon");
        icon.setMouseTransparent(true);
        Button button = new CaptionButton();
        button.setFocusTraversable(false);
        button.setGraphic(icon);
        button.setAccessibleText(accessibleName);
        button.setTooltip(new Tooltip(accessibleName));
        button.getStyleClass().add("ocean-caption-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private static boolean interactive(Object target) {
        if (!(target instanceof Node node)) return false;
        while (node != null) {
            if (node instanceof ButtonBase || node instanceof TextInputControl || node instanceof ComboBoxBase<?>) return true;
            node = node.getParent();
        }
        return false;
    }

    private static void installResize(Stage stage, Scene scene) {
        final boolean[] edges = new boolean[4];
        final double[] start = new double[2];
        final Rectangle2D[] original = new Rectangle2D[1];
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (stage.isMaximized()) { scene.setCursor(Cursor.DEFAULT); return; }
            boolean left = event.getSceneX() < EDGE;
            boolean right = event.getSceneX() > scene.getWidth() - EDGE;
            boolean top = event.getSceneY() < EDGE;
            boolean bottom = event.getSceneY() > scene.getHeight() - EDGE;
            scene.setCursor(left && top ? Cursor.NW_RESIZE : right && top ? Cursor.NE_RESIZE
                    : left && bottom ? Cursor.SW_RESIZE : right && bottom ? Cursor.SE_RESIZE
                    : left ? Cursor.W_RESIZE : right ? Cursor.E_RESIZE
                    : top ? Cursor.N_RESIZE : bottom ? Cursor.S_RESIZE : Cursor.DEFAULT);
        });
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (stage.isMaximized() || event.getButton() != MouseButton.PRIMARY) return;
            edges[0] = event.getSceneX() < EDGE;
            edges[1] = event.getSceneX() > scene.getWidth() - EDGE;
            edges[2] = event.getSceneY() < EDGE;
            edges[3] = event.getSceneY() > scene.getHeight() - EDGE;
            if (!(edges[0] || edges[1] || edges[2] || edges[3])) return;
            original[0] = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            start[0] = event.getScreenX(); start[1] = event.getScreenY();
            event.consume();
        });
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (original[0] == null) return;
            Rectangle2D bounds = resizeBounds(original[0], event.getScreenX() - start[0], event.getScreenY() - start[1],
                    edges[0], edges[1], edges[2], edges[3], stage.getMinWidth(), stage.getMinHeight());
            stage.setX(bounds.getMinX()); stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth()); stage.setHeight(bounds.getHeight());
            event.consume();
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (original[0] != null) { original[0] = null; event.consume(); }
        });
    }

    static Rectangle2D resizeBounds(Rectangle2D original, double dx, double dy,
            boolean left, boolean right, boolean top, boolean bottom, double minWidth, double minHeight) {
        double width = Math.max(minWidth, original.getWidth() + (right ? dx : left ? -dx : 0));
        double height = Math.max(minHeight, original.getHeight() + (bottom ? dy : top ? -dy : 0));
        double x = left ? original.getMaxX() - width : original.getMinX();
        double y = top ? original.getMaxY() - height : original.getMinY();
        return new Rectangle2D(x, y, width, height);
    }
}
