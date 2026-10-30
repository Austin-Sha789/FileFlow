package com.austin.fileflow.support;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.*;

/** Keep JavaFX integration checks independent of the user's registry/preferences. */
public final class MemoryPreferencesFactory implements PreferencesFactory {
    private final Preferences root = new MemoryNode(null, "");
    public Preferences userRoot() { return root; }
    public Preferences systemRoot() { return root; }
    private static final class MemoryNode extends AbstractPreferences {
        private final Map<String, String> values = new HashMap<>();
        private final Map<String, MemoryNode> children = new HashMap<>();
        MemoryNode(AbstractPreferences parent, String name) { super(parent, name); }
        protected void putSpi(String key, String value) { values.put(key, value); }
        protected String getSpi(String key) { return values.get(key); }
        protected void removeSpi(String key) { values.remove(key); }
        protected void removeNodeSpi() { values.clear(); children.clear(); }
        protected String[] keysSpi() { return values.keySet().toArray(String[]::new); }
        protected String[] childrenNamesSpi() { return children.keySet().toArray(String[]::new); }
        protected AbstractPreferences childSpi(String name) { return children.computeIfAbsent(name, value -> new MemoryNode(this, value)); }
        protected void syncSpi() { }
        protected void flushSpi() { }
    }
}
