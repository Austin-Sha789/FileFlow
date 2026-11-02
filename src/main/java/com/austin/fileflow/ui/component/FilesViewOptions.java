package com.austin.fileflow.ui.component;

import static com.austin.fileflow.i18n.I18n.tr;
import java.util.prefs.Preferences;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.util.StringConverter;

/** Presentation preferences are independent from folder, filters, sorting and scan results. */
public final class FilesViewOptions {
    public enum Layout { LIST, DETAILS }
    public enum Size { COMPACT, COMFORTABLE, LARGE }
    private final Preferences preferences;
    private Layout layout;
    private Size size;

    public FilesViewOptions(Preferences preferences) {
        this.preferences = preferences;
        layout = read(Layout.class, "files.layout", Layout.LIST);
        size = read(Size.class, "files.size", Size.COMFORTABLE);
    }
    private <E extends Enum<E>> E read(Class<E> type, String key, E fallback) {
        try { return Enum.valueOf(type, preferences.get(key, fallback.name())); }
        catch (IllegalArgumentException | IllegalStateException | SecurityException ignored) { return fallback; }
    }
    public Layout layout() { return layout; }
    public Size size() { return size; }
    public boolean detailsOnly() { return layout == Layout.DETAILS; }
    public double scale() { return switch (size) { case COMPACT -> .8; case COMFORTABLE -> 1; case LARGE -> 1.4; }; }
    public double rowHeight() { return switch (size) { case COMPACT -> 64; case COMFORTABLE -> 80; case LARGE -> 104; }; }
    private void save() {
        try { preferences.put("files.layout", layout.name()); preferences.put("files.size", size.name()); }
        catch (IllegalStateException | SecurityException ignored) { /* Keep the current session usable. */ }
    }
    public FlowPane controls(Runnable changed) {
        ComboBox<Layout> layouts = choices(Layout.values(), "files.layout.");
        layouts.setId("files-layout-selector"); layouts.setAccessibleText(tr("files.layout.label"));
        layouts.setValue(layout);
        ComboBox<Size> sizes = choices(Size.values(), "files.size.");
        sizes.setId("files-size-selector"); sizes.setAccessibleText(tr("files.size.label"));
        sizes.setValue(size);
        layouts.setOnAction(event -> { layout = layouts.getValue(); save(); changed.run(); });
        sizes.setOnAction(event -> { size = sizes.getValue(); save(); changed.run(); });
        FlowPane bar = new FlowPane(10, 8, new Label(tr("files.layout.label")), layouts,
                new Label(tr("files.size.label")), sizes);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("files-appearance-controls");
        return bar;
    }
    private static <E extends Enum<E>> ComboBox<E> choices(E[] values, String prefix) {
        ComboBox<E> box = new ComboBox<>(); box.getItems().setAll(values);
        box.getStyleClass().add("files-filter-combo");
        box.setConverter(new StringConverter<>() {
            public String toString(E value) { return value == null ? "" : tr(prefix + value.name().toLowerCase(java.util.Locale.ROOT)); }
            public E fromString(String text) { throw new UnsupportedOperationException(); }
        });
        return box;
    }
}
