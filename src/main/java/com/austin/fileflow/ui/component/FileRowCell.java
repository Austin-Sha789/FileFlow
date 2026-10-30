package com.austin.fileflow.ui.component;

import static com.austin.fileflow.i18n.I18n.tr;

import com.austin.fileflow.model.FileItem;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** A reusable full-width file cell in the virtualized file table. */
public final class FileRowCell extends TableCell<FileItem, FileItem> {
    public interface Actions {
        Node preview(FileItem item);
        String location(FileItem item);
        boolean showLocation();
        boolean favourite(Path path);
        void toggleFavourite(FileItem item);
        List<String> tags(FileItem item);
        void addTag(FileItem item);
        void removeTag(FileItem item, String tag);
        boolean selectionMode();
        boolean selected(Path path);
        void select(Path path, boolean selected);
        void animateSelection(CheckBox checkbox);
        void activate(FileItem item);
        ContextMenu menu(Path path);
    }

    private final Actions actions;
    private final Label name = new Label();
    private final Label location = new Label();
    private final Tooltip locationHint = new Tooltip();
    private final StackPane preview = new StackPane();
    private final HBox tagBox = new HBox(6);
    private final Button favourite = new Button();
    private final CheckBox selection = new CheckBox();
    private final HBox row;
    private FileItem rendered;
    private List<String> renderedTags = List.of();

    public FileRowCell(Actions actions) {
        this.actions = actions;
        setText(null);
        name.getStyleClass().add("file-name");
        location.getStyleClass().add("file-relative-path");
        location.setTooltip(locationHint);
        for (Label label : List.of(name, location)) {
            label.setMinWidth(0);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setTextOverrun(OverrunStyle.ELLIPSIS);
        }
        VBox info = new VBox(3, name, location);
        info.setMinWidth(0);
        HBox.setHgrow(info, Priority.ALWAYS);
        preview.setMinSize(56, 42);
        preview.setPrefSize(56, 42);
        preview.setMaxSize(56, 42);
        preview.setMouseTransparent(true);
        preview.getStyleClass().add("ocean-row-preview");
        favourite.getStyleClass().add("favourite-button");
        favourite.setOnAction(event -> {
            if (getItem() == null) return;
            actions.toggleFavourite(getItem());
            updateState();
        });
        Button tag = new Button(tr("ui.tag"));
        tag.getStyleClass().add("add-tag-button");
        tag.setOnAction(event -> { if (getItem() != null) actions.addTag(getItem()); });
        Button more = new Button("⋯");
        more.setAccessibleText(tr("ui.more_file_actions"));
        more.getStyleClass().add("more-button");
        more.setOnAction(event -> {
            if (getItem() != null) actions.menu(getItem().getPath()).show(more, Side.BOTTOM, 0, 0);
        });
        HBox buttons = new HBox(8, tag, favourite, more);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setMinWidth(Region.USE_PREF_SIZE);
        buttons.getStyleClass().add("files-row-actions");
        selection.getStyleClass().add("file-select-checkbox");
        selection.setFocusTraversable(false);
        selection.setOnAction(event -> changeSelection(selection.isSelected()));
        row = new HBox(12, selection, preview, info, tagBox, buttons);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinWidth(0);
        row.setMinHeight(68);
        row.setPrefHeight(68);
        row.setMaxHeight(68);
        row.getStyleClass().add("file-row");
        tableViewProperty().addListener((observable, before, after) -> {
            row.prefWidthProperty().unbind();
            if (after != null) row.prefWidthProperty().bind(after.widthProperty().subtract(26));
        });
        row.setOnContextMenuRequested(event -> {
            if (getItem() != null) actions.menu(getItem().getPath()).show(row, event.getScreenX(), event.getScreenY());
            event.consume();
        });
        row.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (getItem() == null || !actions.selectionMode() || event.getButton() != MouseButton.PRIMARY
                    || event.getClickCount() != 1 || onControl(event)) return;
            changeSelection(!actions.selected(normalizedPath()));
            event.consume();
        });
        row.setOnMouseClicked(event -> {
            if (getItem() == null || event.getButton() != MouseButton.PRIMARY || onControl(event)) return;
            if (event.getClickCount() == 2) { actions.activate(getItem()); event.consume(); }
        });
    }

    private boolean onControl(MouseEvent event) {
        Node node = event.getPickResult().getIntersectedNode();
        while (node != null && node != row) {
            if (node instanceof ButtonBase) return true;
            node = node.getParent();
        }
        return false;
    }

    private Path normalizedPath() { return getItem().getPath().toAbsolutePath().normalize(); }

    private void changeSelection(boolean selected) {
        if (getItem() == null) return;
        actions.select(normalizedPath(), selected);
        selection.setSelected(selected);
        actions.animateSelection(selection);
        updateSelectionState();
    }

    @Override protected void updateItem(FileItem item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setAccessibleText(null);
            preview.getChildren().clear();
            rendered = null;
            renderedTags = List.of();
            tagBox.getChildren().clear();
            setGraphic(null);
            return;
        }
        boolean changed = rendered == null || !Objects.equals(rendered.getPath(), item.getPath())
                || rendered.getSizeBytes() != item.getSizeBytes()
                || !Objects.equals(rendered.getLastModified(), item.getLastModified());
        if (changed) {
            name.setText(item.getName());
            setAccessibleText(item.getName());
            preview.getChildren().setAll(actions.preview(item));
        }
        location.setVisible(actions.showLocation());
        location.setManaged(actions.showLocation());
        if (actions.showLocation()) {
            location.setText(actions.location(item));
            locationHint.setText(item.getPath().toString());
        }
        rendered = item;
        updateState();
        setGraphic(row);
    }

    private void updateSelectionState() {
        boolean mode = actions.selectionMode();
        boolean selected = getItem() != null && mode && actions.selected(normalizedPath());
        selection.setVisible(mode);
        selection.setManaged(mode);
        selection.setSelected(selected);
        boolean styled = row.getStyleClass().contains("file-row-multi-selected");
        if (selected && !styled) row.getStyleClass().add("file-row-multi-selected");
        else if (!selected && styled) row.getStyleClass().remove("file-row-multi-selected");
    }

    private void updateState() {
        FileItem item = getItem();
        if (item == null) return;
        boolean starred = actions.favourite(item.getPath());
        favourite.setText(starred ? "★" : "☆");
        favourite.setAccessibleText(tr(starred ? "files.favourite.remove_accessible" : "files.favourite.add_accessible", item.getName()));
        String starStyle = starred ? "is-favourite" : "not-favourite";
        if (!favourite.getStyleClass().contains(starStyle)) {
            favourite.getStyleClass().removeAll("is-favourite", "not-favourite");
            favourite.getStyleClass().add(starStyle);
        }
        updateSelectionState();
        List<String> tags = actions.tags(item);
        if (!tags.equals(renderedTags)) {
            tagBox.getChildren().clear();
            renderedTags = List.copyOf(tags);
            for (String tag : tags) {
                Label label = new Label(tag);
                label.getStyleClass().add("file-tag-text");
                Button remove = new Button("×");
                remove.getStyleClass().add("ocean-chip-remove");
                remove.setAccessibleText(tr("files.tag.remove_accessible", tag));
                remove.setFocusTraversable(false);
                remove.setOnAction(event -> { if (getItem() != null) actions.removeTag(getItem(), tag); });
                HBox chip = new HBox(5, label, remove);
                chip.setAlignment(Pos.CENTER_LEFT);
                chip.getStyleClass().add("file-tag-chip");
                tagBox.getChildren().add(chip);
            }
        }
    }
}
