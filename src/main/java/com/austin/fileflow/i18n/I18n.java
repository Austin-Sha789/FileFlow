package com.austin.fileflow.i18n;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.util.StringConverter;

/** Application-owned UTF-8 copy; display text never becomes an internal command or filter ID. */
public final class I18n {
    public static final String LANGUAGE_KEY = "ui.language";
    private static final String BUNDLE = "i18n.messages";
    private final Preferences preferences;
    private final ReadOnlyObjectWrapper<AppLanguage> language;
    private volatile AppLanguage current;
    private volatile ResourceBundle messages;
    private static final class Shared {
        private static final I18n INSTANCE = loadDefault();
        private static I18n loadDefault() {
            try { return new I18n(Preferences.userNodeForPackage(I18n.class)); }
            catch (SecurityException | IllegalStateException unavailable) { return new I18n(null, true); }
        }
    }
    public I18n(Preferences preferences) { this(Objects.requireNonNull(preferences), false); }
    private I18n(Preferences preferences, boolean memoryOnly) {
        this.preferences = preferences;
        current = memoryOnly ? AppLanguage.ENGLISH : AppLanguage.fromTag(preferences.get(LANGUAGE_KEY, AppLanguage.ENGLISH.tag()));
        messages = ResourceBundle.getBundle(BUNDLE, current.locale());
        language = new ReadOnlyObjectWrapper<>(current);
    }
    public static I18n instance() { return Shared.INSTANCE; }
    public AppLanguage language() { return current; }
    public ReadOnlyObjectProperty<AppLanguage> languageProperty() { return language.getReadOnlyProperty(); }
    public boolean setLanguage(AppLanguage selected) {
        Objects.requireNonNull(selected);
        if (selected == current) return true;
        boolean saved = preferences != null;
        if (preferences != null) {
            try { preferences.put(LANGUAGE_KEY, selected.tag()); }
            catch (SecurityException | IllegalStateException failure) { saved = false; }
        }
        messages = ResourceBundle.getBundle(BUNDLE, selected.locale());
        current = selected;
        language.set(selected);
        return saved;
    }
    public String text(String key, Object... arguments) {
        String pattern = messages.getString(key);
        return arguments.length == 0 ? pattern : String.format(current.locale(), pattern, arguments);
    }
    public static String tr(String key, Object... arguments) { return instance().text(key, arguments); }
    public static StringBinding binding(String key, Object... arguments) {
        return Bindings.createStringBinding(() -> tr(key, arguments), instance().languageProperty());
    }
    public static DateTimeFormatter dateTimeFormatter() {
        I18n i18n = instance();
        String pattern = i18n.language() == AppLanguage.SIMPLIFIED_CHINESE ? "yyyy年M月d日 HH:mm" : "dd MMM yyyy, HH:mm";
        return DateTimeFormatter.ofPattern(pattern, i18n.current.locale());
    }
    public static void localizeDialog(javafx.scene.control.Dialog<?> dialog) {
        var pane = dialog.getDialogPane();
        for (var type : pane.getButtonTypes()) {
            String key = type == javafx.scene.control.ButtonType.CANCEL ? "common.cancel"
                    : type == javafx.scene.control.ButtonType.CLOSE ? "common.close"
                    : type == javafx.scene.control.ButtonType.OK ? "common.ok"
                    : type == javafx.scene.control.ButtonType.YES ? "common.yes"
                    : type == javafx.scene.control.ButtonType.NO ? "common.no" : null;
            if (key != null && pane.lookupButton(type) instanceof javafx.scene.control.ButtonBase button) button.setText(tr(key));
        }
        Runnable details = () -> {
            if (pane.lookup(".details-button") instanceof javafx.scene.control.Hyperlink link) {
                link.setText(tr(pane.isExpanded() ? "common.hide_details" : "common.show_details"));
            }
        };
        pane.expandedProperty().addListener((observable, before, after) -> details.run());
        var previous = dialog.getOnShown();
        dialog.setOnShown(event -> { if (previous != null) previous.handle(event); details.run(); });
        details.run();
    }

    /** Formats a known application-owned choice; paths, file names and user tags never pass here. */
    public static String choice(String logicalValue) {
        String key = ChoiceKeys.key(logicalValue);
        return key == null ? logicalValue : tr(key);
    }
    public static void localizeChoices(ComboBox<String> box) {
        box.setConverter(new StringConverter<>() {
            @Override public String toString(String value) { return value == null ? "" : choice(value); }
            @Override public String fromString(String text) {
                for (String value : box.getItems()) if (Objects.equals(text, choice(value))) return value;
                return text;
            }
        });
        box.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty); setText(empty || value == null ? null : choice(value));
                setStyle("-fx-text-fill: #e5efff; -fx-font-family: '" + (instance().language() == AppLanguage.SIMPLIFIED_CHINESE ? "Microsoft YaHei UI" : "Segoe UI") + "';");
            }
        });
    }
}
