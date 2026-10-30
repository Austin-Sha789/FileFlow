package com.austin.fileflow.ui.component;

import javafx.animation.TranslateTransition;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** A keyboard-accessible toggle with a sliding thumb. */
public final class SlideSwitch extends ToggleButton {
    private final Region thumb = new Region();
    private final TranslateTransition motion = new TranslateTransition(Duration.millis(120), thumb);

    public SlideSwitch(String accessibleName) {
        setAccessibleText(accessibleName);
        getStyleClass().add("ocean-slide-switch");
        thumb.getStyleClass().add("ocean-switch-thumb");
        StackPane track = new StackPane(thumb);
        track.getStyleClass().add("ocean-switch-track");
        track.setMouseTransparent(true);
        setGraphic(track);
        thumb.setTranslateX(-9);
        selectedProperty().addListener((observable, before, selected) -> {
            motion.stop();
            if (getScene() == null || getScene().getWindow() == null || !getScene().getWindow().isShowing()) {
                thumb.setTranslateX(selected ? 9 : -9);
            } else {
                motion.setToX(selected ? 9 : -9);
                motion.playFromStart();
            }
        });
    }
}
