package com.austin.fileflow.ui.component;

import static com.austin.fileflow.i18n.I18n.tr;
import com.austin.fileflow.model.FileItem;
import com.austin.fileflow.util.FileSizeFormatter;
import java.util.AbstractList;
import java.util.List;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

/** A virtualized grid: only visible rows create cards and request thumbnails. */
public final class FilesGridView extends ListView<Integer> {
    private final TableView<FileItem> source;
    private final FileRowCell.Actions actions;
    private final FilesViewOptions options;
    private final Consumer<FileItem> inspect;
    private int columns = 1;
    private boolean active;
    public FilesGridView(TableView<FileItem> source, FileRowCell.Actions actions,
                         FilesViewOptions options, Consumer<FileItem> inspect) {
        this.source = source; this.actions = actions; this.options = options; this.inspect = inspect;
        setId("files-grid"); getStyleClass().add("files-grid");
        setPlaceholder(new Label(tr("ui.no_files_in_this_folder")));
        setCellFactory(list -> new GridRow());
        source.getItems().addListener((ListChangeListener<FileItem>) change -> { if (active) rebuild(); });
        widthProperty().addListener((observable, before, after) -> {
            int next = columnCount(after.doubleValue());
            if (next != columns && active) {
                int anchor = firstVisibleItem(); columns = next; rebuild(); scrollTo(anchor / columns);
            }
        });
    }
    private double cardWidth() { return switch (options.size()) { case COMPACT -> 170; case COMFORTABLE -> 220; case LARGE -> 280; }; }
    private double cardHeight() { return switch (options.size()) { case COMPACT -> 200; case COMFORTABLE -> 240; case LARGE -> 290; }; }
    private int columnCount(double width) { return Math.max(1, (int)((width - 24) / (cardWidth() + 12))); }
    public void setActive(boolean value) {
        active = value; setVisible(value); setManaged(value);
        if (active) rebuild(); else setItems(FXCollections.observableArrayList());
    }
    private void rebuild() {
        columns = columnCount(getWidth());
        setFixedCellSize(cardHeight() + 12);
        int rows = (source.getItems().size() + columns - 1) / columns;
        setItems(FXCollections.observableList(new AbstractList<Integer>() {
            public Integer get(int index) { return index; }
            public int size() { return rows; }
        }));
        refresh();
    }
    public void updateAppearance() {
        int anchor = firstVisibleItem(); rebuild(); scrollTo(anchor / columns);
    }
    public int firstVisibleItem() {
        return lookupAll(".list-cell").stream().filter(node -> node instanceof ListCell<?> cell
                        && !cell.isEmpty() && cell.getBoundsInParent().getMaxY() > 0)
                .mapToInt(node -> ((ListCell<?>)node).getIndex() * columns).min().orElse(0);
    }
    public void scrollToItem(int index) { scrollTo(Math.max(0, index) / columns); }
    private final class GridRow extends ListCell<Integer> {
        @Override protected void updateItem(Integer index, boolean empty) {
            super.updateItem(index, empty); setText(null);
            if (empty || index == null || !active) { setGraphic(null); return; }
            HBox row = new HBox(12); row.setAlignment(Pos.TOP_LEFT);
            for (int c = 0; c < columns; c++) {
                int offset = index * columns + c;
                if (offset >= source.getItems().size()) break;
                row.getChildren().add(card(source.getItems().get(offset)));
            }
            setGraphic(row);
        }
    }
    private VBox card(FileItem item) {
        int width = (int)cardWidth(); int previewHeight = (int)cardHeight() - 110;
        Node preview = actions.preview(item, width - 24, previewHeight);
        StackPane cover = new StackPane(preview); cover.setMinSize(width - 24, previewHeight);
        cover.setPrefSize(width - 24, previewHeight); cover.setMaxSize(width - 24, previewHeight);
        cover.setMouseTransparent(true);
        Label name = new Label(item.getName()); name.getStyleClass().add("file-name"); name.setMaxWidth(width - 24);
        name.setTooltip(new Tooltip(item.getPath().toString()));
        Label size = new Label(item.isDirectory() ? tr("files.kind.folder") : FileSizeFormatter.format(item.getSizeBytes()));
        size.getStyleClass().add("file-relative-path");
        CheckBox selection = new CheckBox(); selection.setAccessibleText(item.getName());
        selection.setVisible(actions.selectionMode()); selection.setManaged(actions.selectionMode());
        selection.setSelected(actions.selected(item.getPath().toAbsolutePath().normalize()));
        selection.setOnAction(event -> { actions.select(item.getPath().toAbsolutePath().normalize(), selection.isSelected()); refresh(); });
        Button star = new Button(actions.favourite(item.getPath()) ? "★" : "☆");
        star.getStyleClass().addAll("favourite-button", actions.favourite(item.getPath()) ? "is-favourite" : "not-favourite");
        star.setAccessibleText(tr(actions.favourite(item.getPath()) ? "files.favourite.remove_accessible" : "files.favourite.add_accessible", item.getName()));
        star.setOnAction(event -> { actions.toggleFavourite(item); refresh(); });
        Button tag = new Button(tr("ui.tag")); tag.getStyleClass().add("add-tag-button");
        tag.setOnAction(event -> { actions.addTag(item); refresh(); });
        Button more = new Button("⋯"); more.getStyleClass().add("more-button"); more.setAccessibleText(tr("ui.more_file_actions"));
        more.setOnAction(event -> actions.menu(item.getPath()).show(more, Side.BOTTOM, 0, 0));
        HBox buttons = new HBox(5, selection, tag, star, more); buttons.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(5, cover, name, size, buttons);
        card.setMinSize(width, cardHeight()); card.setPrefSize(width, cardHeight()); card.setMaxSize(width, cardHeight());
        card.getStyleClass().add("files-grid-card");
        if (actions.selectionMode() && actions.selected(item.getPath().toAbsolutePath().normalize())) card.getStyleClass().add("file-row-multi-selected");
        String tags = String.join(", ", actions.tags(item));
        if (!tags.isBlank()) Tooltip.install(size, new Tooltip(tags));
        card.setFocusTraversable(true); card.setAccessibleText(item.getName());
        card.setOnContextMenuRequested(event -> { actions.menu(item.getPath()).show(card, event.getScreenX(), event.getScreenY()); event.consume(); });
        card.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY || onButton(event.getTarget(), card)) return;
            if (event.getClickCount() == 2) actions.activate(item);
            else if (actions.selectionMode()) {
                actions.select(item.getPath().toAbsolutePath().normalize(), !actions.selected(item.getPath().toAbsolutePath().normalize())); refresh();
            } else inspect.accept(item);
        });
        card.setOnKeyPressed(event -> {
            if (onButton(event.getTarget(), card)) return;
            if (event.getCode() == KeyCode.ENTER) { actions.activate(item); event.consume(); }
            else if (event.getCode() == KeyCode.SPACE) {
                if (actions.selectionMode()) { actions.select(item.getPath().toAbsolutePath().normalize(), !actions.selected(item.getPath().toAbsolutePath().normalize())); refresh(); }
                else inspect.accept(item);
                event.consume();
            }
        });
        return card;
    }
    private static boolean onButton(Object target, Node root) {
        for (Node node = target instanceof Node n ? n : null; node != null && node != root; node = node.getParent())
            if (node instanceof ButtonBase) return true;
        return false;
    }
}
