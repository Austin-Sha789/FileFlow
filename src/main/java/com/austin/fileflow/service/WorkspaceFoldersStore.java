package com.austin.fileflow.service;

import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;

/** The saved folder library is independent of the deduplicated plan used for a batch scan. */
public final class WorkspaceFoldersStore {
    private final Preferences preferences;
    public WorkspaceFoldersStore() { this(defaultPreferences()); }
    public WorkspaceFoldersStore(Preferences preferences) { this.preferences = preferences; }
    private static Preferences defaultPreferences() {
        try { return Preferences.userNodeForPackage(WorkspaceFoldersStore.class).node("workspace-folders"); }
        catch (SecurityException | IllegalStateException unavailable) { return null; }
    }
    public List<Path> load() {
        if (preferences == null) return List.of();
        List<Path> folders = new ArrayList<>();
        try {
            int count = Math.min(1000, Math.max(0, preferences.getInt("count", 0)));
            for (int index = 0; index < count; index++) {
                String saved = preferences.get("folder." + index, "");
                if (saved.isBlank()) continue;
                try {
                    Path folder = Path.of(saved).toAbsolutePath().normalize();
                    if (!folders.contains(folder)) folders.add(folder);
                } catch (java.nio.file.InvalidPathException ignored) { }
            }
        } catch (SecurityException | IllegalStateException unavailable) { return List.of(); }
        return List.copyOf(folders);
    }
    public boolean save(List<Path> folders) {
        if (preferences == null || folders.size() > 1000) return false;
        try {
            List<String> values = folders.stream().map(folder -> folder.toAbsolutePath().normalize().toString()).toList();
            if (values.stream().anyMatch(value -> value.length() > Preferences.MAX_VALUE_LENGTH)) return false;
            int previous = Math.min(1000, Math.max(0, preferences.getInt("count", 0)));
            for (int index = 0; index < values.size(); index++) preferences.put("folder." + index, values.get(index));
            preferences.putInt("count", values.size());
            for (int index = values.size(); index < previous; index++) preferences.remove("folder." + index);
            return true;
        } catch (SecurityException | IllegalStateException | IllegalArgumentException unavailable) { return false; }
    }
}
