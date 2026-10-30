package com.austin.fileflow.ui;

import static com.austin.fileflow.i18n.I18n.tr;

import com.austin.fileflow.model.FileType;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.AccessibleRole;
import javafx.scene.AccessibleAction;
import javafx.scene.input.KeyCode;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/** Small reusable components for the Ocean desktop presentation. */
public final class OceanComponents {
    private OceanComponents() { }

    public static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    public static Node icon(String name, double size) {
        String path = switch (name) {
            case "search" -> "M21 21 L16.7 16.7 M19 11 A8 8 0 1 1 3 11 A8 8 0 1 1 19 11";
            case "scan" -> "M8 3 H4 A1 1 0 0 0 3 4 V8 M16 3 H20 A1 1 0 0 1 21 4 V8 M3 16 V20 A1 1 0 0 0 4 21 H8 M16 21 H20 A1 1 0 0 0 21 20 V16 M8 12 H16 M12 8 V16";
            case "copy" -> "M9 8 H20 A1 1 0 0 1 21 9 V20 A1 1 0 0 1 20 21 H9 A1 1 0 0 1 8 20 V9 A1 1 0 0 1 9 8 M4 16 H3 V3 H16 V4";
            case "star" -> "M12 3 L14.8 8.7 L21 9.6 L16.5 14 L17.6 20.2 L12 17.3 L6.4 20.2 L7.5 14 L3 9.6 L9.2 8.7 Z";
            case "clock" -> "M21 12 A9 9 0 1 1 3 12 A9 9 0 1 1 21 12 M12 7 V12 L16 14";
            case "tag" -> "M3 3 H12 L21 12 L12 21 L3 12 Z M7 7 H7.1";
            case "drive" -> "M5 4 H19 L22 15 V20 H2 V15 Z M2 15 H22 M6 18 H6.1 M10 18 H10.1";
            case "refresh" -> "M20 7 A9 9 0 1 0 21 14 M20 3 V8 H15";
            case "arrow" -> "M5 12 H19 M13 6 L19 12 L13 18";
            case "back" -> "M15 5 L8 12 L15 19";
            case "close" -> "M6 6 L18 18 M18 6 L6 18";
            case "chevron" -> "M6 9 L12 15 L18 9";
            case "check" -> "M5 12 L10 17 L20 7";
            case "plus" -> "M12 4 V20 M4 12 H20";
            default -> "M3 7 V19 A2 2 0 0 0 5 21 H19 A2 2 0 0 0 21 19 V9 A2 2 0 0 0 19 7 H12 L10 4 H5 A2 2 0 0 0 3 6 Z";
        };
        SVGPath shape = new SVGPath();
        shape.setContent(path);
        shape.setFill(Color.TRANSPARENT);
        shape.setStrokeLineCap(StrokeLineCap.ROUND);
        shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
        shape.getStyleClass().add("ocean-line-icon");
        shape.setScaleX(size / 24.0);
        shape.setScaleY(size / 24.0);
        StackPane box = new StackPane(new Group(shape));
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        box.setMouseTransparent(true);
        return box;
    }

    public static Button action(String title, String icon, String style, Runnable action) {
        Button button = new Button(title, icon(icon, 17));
        button.getStyleClass().add(style);
        button.setOnAction(event -> action.run());
        return button;
    }

    public static HBox section(String text, String actionText, Runnable action) {
        Label title = label(text, "section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, title, spacer);
        row.setAlignment(Pos.CENTER_LEFT);
        if (action != null) {
            row.getChildren().add(action(actionText, "arrow", "ocean-text-button", action));
        }
        return row;
    }

    public static VBox tile(String title, String description, String icon, Runnable action) {
        Label heading = label(title, "ocean-tile-title");
        Label detail = label(description, "ocean-muted");
        detail.setWrapText(true);
        VBox content = new VBox(9, icon(icon, 24), heading, detail);
        return card(content, "ocean-action-tile", title, action);
    }

    private static final class ActionCard extends VBox {
        private final Runnable action;
        ActionCard(Runnable action) { this.action = action; }
        @Override
        public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
            if (action == AccessibleAction.FIRE) this.action.run();
            else super.executeAccessibleAction(action, parameters);
        }
    }

    public static VBox card(VBox original, String style, String accessibleText, Runnable action) {
        ActionCard content = new ActionCard(action);
        content.setSpacing(original.getSpacing());
        java.util.List<Node> children = java.util.List.copyOf(original.getChildren());
        original.getChildren().clear();
        content.getChildren().addAll(children);
        content.setAlignment(Pos.TOP_LEFT);
        content.setMinWidth(0);
        content.setPrefWidth(0);
        content.setMaxWidth(Double.MAX_VALUE);
        content.setMaxHeight(Region.USE_PREF_SIZE);
        content.getStyleClass().add(style);
        content.setFocusTraversable(true);
        content.setAccessibleRole(AccessibleRole.BUTTON);
        content.setAccessibleText(accessibleText);
        content.setOnMouseClicked(event -> action.run());
        content.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                action.run();
                event.consume();
            }
        });
        HBox.setHgrow(content, Priority.ALWAYS);
        return content;
    }

    public static VBox empty(String title, String description, Runnable scan) {
        Label heading = label(title, "ocean-empty-title");
        Label detail = label(description, "ocean-muted");
        detail.setWrapText(true);
        detail.setMaxWidth(390);
        detail.setAlignment(Pos.CENTER);
        VBox pane = new VBox(14, icon("folder", 42), heading, detail);
        pane.setAlignment(Pos.CENTER);
        pane.getStyleClass().add("ocean-empty");
        if (scan != null) pane.getChildren().add(action(tr("ui.scan_a_folder"), "scan", "primary-button", scan));
        return pane;
    }

    public static StackPane heroArt() {
        Circle orbit = new Circle(100);
        orbit.getStyleClass().add("ocean-hero-orbit");
        SVGPath back = new SVGPath();
        back.setContent("M10 42 V22 Q10 12 20 12 H70 L85 30 H154 Q164 30 164 40 V128 Q164 138 154 138 H20 Q10 138 10 128 Z");
        back.getStyleClass().add("ocean-folder-back");
        back.setRotate(9);
        back.setTranslateX(18);
        back.setTranslateY(-8);
        SVGPath front = new SVGPath();
        front.setContent("M8 40 V30 Q8 20 18 20 H62 L80 37 H151 Q162 37 162 48 V132 Q162 143 151 143 H18 Q8 143 8 132 Z");
        front.getStyleClass().add("ocean-folder-front");
        front.setRotate(-10);
        StackPane art = new StackPane(orbit, new Group(back, front));
        art.setMinSize(200, 170);
        art.setPrefSize(220, 180);
        art.setMaxSize(240, 190);
        art.setMouseTransparent(true);
        return art;
    }

    public static StackPane donut(Map<FileType, Long> bytesByType, String total) {
        PieChart chart = new PieChart();
        chart.setLegendVisible(false);
        chart.setLabelsVisible(false);
        chart.setAnimated(false);
        chart.setStartAngle(90);
        bytesByType.entrySet().stream().sorted(Map.Entry.<FileType, Long>comparingByValue().reversed())
                .filter(entry -> entry.getValue() > 0)
                .forEach(entry -> chart.getData().add(new PieChart.Data(entry.getKey().name(), entry.getValue())));
        if (chart.getData().isEmpty()) chart.getData().add(new PieChart.Data(tr("ui.empty"), 1));
        chart.getStyleClass().add("ocean-donut");
        chart.setMinSize(150, 150);
        chart.setPrefSize(160, 160);
        chart.setMaxSize(160, 160);
        Circle hole = new Circle(48);
        hole.getStyleClass().add("ocean-donut-hole");
        Label size = label(total, "ocean-donut-total");
        Label caption = label(tr("ui.in_this_scan"), "ocean-muted");
        VBox text = new VBox(3, size, caption);
        text.setAlignment(Pos.CENTER);
        StackPane pane = new StackPane(chart, hole, text);
        pane.setMinSize(160, 160);
        pane.setMaxSize(160, 160);
        return pane;
    }
}
