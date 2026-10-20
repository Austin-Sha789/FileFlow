package com.austin.fileflow.service;

import com.austin.fileflow.model.ShortcutAction;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.prefs.Preferences;

public class ShortcutManager {

    private static final String PREFERENCE_PREFIX =
            "shortcut.";

    private final Preferences preferences =
            Preferences.userNodeForPackage(ShortcutManager.class);

    private final Map<ShortcutAction, KeyCombination> shortcuts =
            new EnumMap<>(ShortcutAction.class);

    public ShortcutManager() {
        loadShortcuts();
    }

    private void loadShortcuts() {
        for (ShortcutAction action : ShortcutAction.values()) {

            String savedShortcut =
                    preferences.get(
                            getPreferenceKey(action),
                            getDefaultShortcut(action).getName()
                    );

            shortcuts.put(
                    action,
                    KeyCombination.valueOf(savedShortcut)
            );
        }
    }

    public KeyCombination getShortcut(
            ShortcutAction action
    ) {
        return shortcuts.get(action);
    }

    public boolean matches(
            ShortcutAction action,
            KeyEvent event
    ) {

        KeyCombination shortcut =
                shortcuts.get(action);

        return shortcut != null
                && shortcut.match(event);
    }

    public ShortcutAction findConflict(
            ShortcutAction action,
            KeyCombination shortcut
    ) {

        for (Map.Entry<ShortcutAction, KeyCombination> entry
                : shortcuts.entrySet()) {

            if (entry.getKey() != action
                    && entry.getValue().equals(shortcut)) {

                return entry.getKey();
            }
        }

        return null;
    }

    public void setShortcut(
            ShortcutAction action,
            KeyCombination shortcut
    ) {

        shortcuts.put(
                action,
                shortcut
        );

        preferences.put(
                getPreferenceKey(action),
                shortcut.getName()
        );
    }

    public void resetToDefaults() {

        for (ShortcutAction action : ShortcutAction.values()) {

            KeyCombination defaultShortcut =
                    getDefaultShortcut(action);

            shortcuts.put(
                    action,
                    defaultShortcut
            );

            preferences.put(
                    getPreferenceKey(action),
                    defaultShortcut.getName()
            );
        }
    }

    private String getPreferenceKey(
            ShortcutAction action
    ) {
        return PREFERENCE_PREFIX
                + action.name();
    }

    private KeyCombination getDefaultShortcut(
            ShortcutAction action
    ) {

        return switch (action) {

            case REFRESH_SCAN ->
                    KeyCombination.valueOf(
                            "Shortcut+R"
                    );

            case FOCUS_SEARCH ->
                    KeyCombination.valueOf(
                            "Shortcut+F"
                    );

            case SCAN_FOLDER ->
                    KeyCombination.valueOf(
                            "Shortcut+Shift+O"
                    );
        };
    }
}