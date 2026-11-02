package com.austin.fileflow.ui.component;

import static com.austin.fileflow.i18n.I18n.tr;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

/** An editable temporary analysis selection. Nothing is applied until Analyse is pressed. */
public final class AnalysisScopeDialog extends Dialog<List<Path>> {
    public AnalysisScopeDialog() {
        setTitle(tr("analysis.choose_scope")); setHeaderText(null); setResizable(true);
        var paths = FXCollections.<Path>observableArrayList();
        ListView<Path> list = new ListView<>(paths); list.setId("analysis-scope-list");
        list.setPrefHeight(230); list.setPlaceholder(new Label(tr("analysis.scope_empty")));
        list.setCellFactory(view -> new ListCell<>() {
            protected void updateItem(Path path, boolean empty) {
                super.updateItem(path, empty); setText(empty || path == null ? null : path.toString());
                setTooltip(empty || path == null ? null : new Tooltip(path.toString()));
            }
        });
        java.util.function.Consumer<List<File>> add = files -> {
            if (files == null) return;
            for (File file : files) {
                Path path = file.toPath().toAbsolutePath().normalize();
                if (!paths.contains(path)) paths.add(path);
            }
        };
        Button files = new Button(tr("analysis.add_files")); files.getStyleClass().add("secondary-button");
        files.setId("analysis-add-files");
        files.setOnAction(event -> {
            FileChooser chooser = new FileChooser(); chooser.setTitle(tr("analysis.add_files"));
            add.accept(chooser.showOpenMultipleDialog(getDialogPane().getScene().getWindow()));
        });
        Button folder = new Button(tr("ui.add_folder")); folder.getStyleClass().add("secondary-button");
        folder.setId("analysis-add-folder");
        folder.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser(); chooser.setTitle(tr("analysis.add_folder"));
            File selected = chooser.showDialog(getDialogPane().getScene().getWindow());
            if (selected != null) add.accept(List.of(selected));
        });
        Button remove = new Button(tr("ui.remove")); remove.getStyleClass().add("secondary-button");
        remove.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        remove.setOnAction(event -> paths.remove(list.getSelectionModel().getSelectedItem()));
        HBox buttons = new HBox(10, files, folder, remove); buttons.setAlignment(Pos.CENTER_LEFT);
        Label description = new Label(tr("analysis.scope_description")); description.setWrapText(true);
        Label count = new Label(); count.setText(tr("analysis.scope_count", 0));
        paths.addListener((javafx.collections.ListChangeListener<Path>) change -> count.setText(tr("analysis.scope_count", paths.size())));
        VBox content = new VBox(12, description, buttons, count, list); content.setPrefSize(620, 340);
        VBox.setVgrow(list, Priority.ALWAYS);
        content.setOnDragOver(event -> { if (event.getDragboard().hasFiles()) event.acceptTransferModes(TransferMode.COPY); event.consume(); });
        content.setOnDragDropped(event -> { boolean hasFiles = event.getDragboard().hasFiles(); if (hasFiles) add.accept(event.getDragboard().getFiles()); event.setDropCompleted(hasFiles); event.consume(); });
        getDialogPane().setContent(content);
        ButtonType analyse = new ButtonType(tr("analysis.start"), ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(analyse, ButtonType.CANCEL);
        getDialogPane().lookupButton(analyse).disableProperty().bind(javafx.beans.binding.Bindings.isEmpty(paths));
        setResultConverter(button -> button == analyse ? List.copyOf(paths) : null);
    }
}
