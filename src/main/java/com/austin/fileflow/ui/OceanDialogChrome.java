package com.austin.fileflow.ui;

import static com.austin.fileflow.i18n.I18n.tr;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

/** Uses the same dark chrome for alerts, confirmations and app dialogs. */
public final class OceanDialogChrome {
    private OceanDialogChrome() { }
    public static void install(Dialog<?> dialog) {
        install(dialog, OceanWindowChrome.isMac());
    }

    static void install(Dialog<?> dialog, boolean mac) {
        var pane = dialog.getDialogPane();
        if (pane.getProperties().putIfAbsent("ocean-chrome", Boolean.TRUE) != null) return;
        if (mac) {
            dialog.initStyle(StageStyle.DECORATED);
            // Native macOS titlebar owns its controls; preserve the dialog's content/header.
            return;
        }
        dialog.initStyle(StageStyle.UNDECORATED);
        Label caption = new Label(dialog.getTitle());
        caption.getStyleClass().add("ocean-dialog-caption");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button close = new Button("×");
        close.setAccessibleText(tr("ui.close_dialog"));
        close.getStyleClass().add("ocean-dialog-close");
        close.setOnAction(event -> dialog.close());
        HBox titlebar = new HBox(9, AppIcon.view(18), caption, spacer, close);
        titlebar.setAlignment(Pos.CENTER_LEFT);
        titlebar.getStyleClass().add("ocean-dialog-titlebar");
        final double[] offset = new double[2];
        titlebar.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            var window = pane.getScene().getWindow();
            offset[0] = event.getScreenX() - window.getX();
            offset[1] = event.getScreenY() - window.getY();
        });
        titlebar.setOnMouseDragged(event -> {
            if (!event.isPrimaryButtonDown() || event.getTarget() == close) return;
            var window = pane.getScene().getWindow();
            window.setX(event.getScreenX() - offset[0]); window.setY(event.getScreenY() - offset[1]);
        });
        Node header = pane.getHeader();
        if (header == null && pane.getHeaderText() != null) {
            Label heading = new Label(pane.getHeaderText());
            heading.setWrapText(true); heading.setMaxWidth(600);
            heading.getStyleClass().add("ocean-dialog-heading");
            header = heading;
        }
        pane.setHeaderText(null);
        VBox headers = new VBox(22, titlebar);
        if (header != null) headers.getChildren().add(header);
        pane.setHeader(headers);
    }
}
