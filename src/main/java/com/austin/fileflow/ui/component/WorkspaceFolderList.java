package com.austin.fileflow.ui.component;

import static com.austin.fileflow.i18n.I18n.tr;
import com.austin.fileflow.ui.OceanComponents;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import javafx.animation.*;
import javafx.css.PseudoClass;
import javafx.scene.AccessibleRole;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/** Only the saved-folder area scrolls; the dot rail maps one marker to each folder. */
public final class WorkspaceFolderList extends HBox {
    private static final double ROW_HEIGHT = 32;
    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");
    private final VBox rows = new VBox();
    private final ScrollPane scroll = new ScrollPane(rows);
    private final Pane rail = new Pane() { @Override protected void layoutChildren() { positionDots(); } };
    private final List<Circle> dots = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();
    private final Map<Circle, Timeline> animations = new HashMap<>();
    private final PauseTransition scrollRest = new PauseTransition(Duration.millis(240));
    private record DotState(double radius, double opacity, Color color) { }
    private final Map<Circle, DotState> dotStates = new HashMap<>();
    private Timeline scrollAnimation;
    private List<Path> folders = List.of();
    private Path active;
    private Consumer<Path> activate = path -> { };
    private int hovered = -1;
    private boolean scrolling;

    public WorkspaceFolderList() {
        getStyleClass().add("workspace-folder-list");
        setMinHeight(0); setPrefHeight(150); setMaxHeight(Double.MAX_VALUE);
        scroll.getStyleClass().add("workspace-folder-scroll");
        scroll.setFitToWidth(true); scroll.setMinHeight(0); scroll.setMinWidth(0);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        HBox.setHgrow(scroll, Priority.ALWAYS);
        rail.getStyleClass().add("workspace-dot-rail");
        rail.setMinWidth(12); rail.setPrefWidth(12); rail.setMaxWidth(12);
        getChildren().addAll(scroll, rail);
        scroll.vvalueProperty().addListener((observable, before, after) -> {
            scrolling = true; updateDots(); scrollRest.playFromStart();
        });
        scroll.viewportBoundsProperty().addListener(observable -> rail.requestLayout());
        scrollRest.setOnFinished(event -> { scrolling = false; updateDots(); });
        sceneProperty().addListener((observable, before, after) -> {
            if (after == null) { scrollRest.stop(); if (scrollAnimation != null) scrollAnimation.stop(); animations.values().forEach(Timeline::stop); }
        });
    }
    public void setFolders(List<Path> paths, Path selected, Consumer<Path> onActivate) {
        activate = onActivate; active = selected;
        if (!folders.equals(paths)) {
            animations.values().forEach(Timeline::stop); animations.clear(); dotStates.clear();
            folders = List.copyOf(paths); rows.getChildren().clear(); rail.getChildren().clear(); dots.clear(); buttons.clear(); hovered = -1;
            for (int index = 0; index < folders.size(); index++) {
                final int position = index; Path path = folders.get(index);
                String name = path.getFileName() == null ? path.toString() : path.getFileName().toString();
                Button button = OceanComponents.action(name, "folder", "ocean-folder-shortcut", () -> activate.accept(path));
                button.setMinHeight(ROW_HEIGHT); button.setPrefHeight(ROW_HEIGHT); button.setMaxHeight(ROW_HEIGHT);
                button.setMinWidth(0); button.setMaxWidth(Double.MAX_VALUE); button.setTooltip(new Tooltip(path.toString()));
                button.hoverProperty().addListener((observable, before, after) -> { hovered = after ? position : -1; updateDots(); });
                Circle dot = new Circle(2, Color.web("#617e9f"));
                dot.getStyleClass().add("workspace-folder-dot"); dot.setFocusTraversable(true); dot.setAccessibleRole(AccessibleRole.BUTTON);
                dot.setOnMouseClicked(event -> { scrollTo(position); event.consume(); });
                dot.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) { scrollTo(position); event.consume(); } });
                dot.hoverProperty().addListener((observable, before, after) -> { hovered = after ? position : -1; updateDots(); });
                Tooltip.install(dot, new Tooltip(path.toString()));
                buttons.add(button); rows.getChildren().add(button); dots.add(dot); rail.getChildren().add(dot);
            }
            rail.requestLayout();
        }
        for (int index = 0; index < buttons.size(); index++) {
            buttons.get(index).pseudoClassStateChanged(ACTIVE, folders.get(index).equals(active));
            dots.get(index).setAccessibleText(tr("workspace.scroll_to", buttons.get(index).getText()));
        }
        updateDots();
    }
    private void positionDots() {
        double height = Math.max(0, rail.getHeight());
        boolean fits = folders.size() * ROW_HEIGHT <= height;
        for (int index = 0; index < dots.size(); index++) {
            double y = fits ? ROW_HEIGHT * (index + .5) : 8 + Math.max(0, height - 16) * index / Math.max(1, dots.size() - 1);
            dots.get(index).setCenterX(6); dots.get(index).setCenterY(Math.min(Math.max(0, height - 3), y));
        }
        updateDots();
    }
    private void scrollTo(int index) {
        double overflow = Math.max(0, rows.getHeight() - scroll.getViewportBounds().getHeight());
        double target = overflow == 0 ? 0 : Math.max(0, Math.min(1,
                (index * ROW_HEIGHT - (scroll.getViewportBounds().getHeight() - ROW_HEIGHT) / 2) / overflow));
        if (scrollAnimation != null) scrollAnimation.stop();
        scrollAnimation = new Timeline(new KeyFrame(Duration.millis(220), new KeyValue(scroll.vvalueProperty(), target, Interpolator.EASE_BOTH)));
        scrollAnimation.play();
    }
    private void updateDots() {
        int moving = (int) Math.round(scroll.getVvalue() * Math.max(0, folders.size() - 1));
        for (int index = 0; index < dots.size(); index++) {
            Circle dot = dots.get(index);
            boolean selected = folders.get(index).equals(active);
            boolean hot = index == hovered || (scrolling && index == moving);
            double spacing = Math.max(0, rail.getHeight() - 16) / Math.max(1, dots.size() - 1);
            double scale = Math.min(1, Math.max(.25, spacing / 8));
            double radius = (hot ? 3.8 : selected ? 3 : 2) * scale;
            double opacity = hot || selected ? 1 : scrolling ? .75 : .45;
            Color color = Color.web(hot ? "#c3e2ff" : selected ? "#6eafff" : "#617e9f");
            DotState state = new DotState(radius, opacity, color);
            if (state.equals(dotStates.put(dot, state))) continue;
            Timeline previous = animations.remove(dot); if (previous != null) previous.stop();
            Timeline animation = new Timeline(new KeyFrame(Duration.millis(130),
                    new KeyValue(dot.radiusProperty(), radius, Interpolator.EASE_BOTH),
                    new KeyValue(dot.opacityProperty(), opacity, Interpolator.EASE_BOTH),
                    new KeyValue(dot.fillProperty(), color, Interpolator.EASE_BOTH)));
            animations.put(dot, animation); animation.play();
        }
    }
}
