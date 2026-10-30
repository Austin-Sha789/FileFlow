package com.austin.fileflow.i18n;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.AbstractPreferences;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class I18nTest {
    private static final Pattern PRINTF = Pattern.compile("%(?:\\d+\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?[a-zA-Z%]");
    private static List<String> arguments(String text) {
        List<String> arguments = new ArrayList<>();
        var matcher = PRINTF.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            if (!token.equals("%%") && !token.equals("%n")) arguments.add(token);
        }
        return arguments;
    }
    @Test void newInstallStartsInEnglishRegardlessOfSystemLocale() {
        var i18n = new I18n(new MemoryPreferences());
        assertEquals(AppLanguage.ENGLISH, i18n.language());
        assertEquals("Home", i18n.text("nav.home"));
    }
    @Test void selectedLanguagePersistsWhenTheServiceIsRecreated() {
        var preferences = new MemoryPreferences();
        var first = new I18n(preferences);
        assertTrue(first.setLanguage(AppLanguage.SIMPLIFIED_CHINESE));
        var restarted = new I18n(preferences);
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, restarted.language());
        assertEquals("首页", restarted.text("nav.home"));
        assertTrue(restarted.setLanguage(AppLanguage.ENGLISH));
        assertEquals("en", preferences.get(I18n.LANGUAGE_KEY, ""));
    }
    @Test void unsupportedSavedLanguageFallsBackToEnglish() {
        var preferences = new MemoryPreferences(); preferences.put(I18n.LANGUAGE_KEY, "unsupported");
        assertEquals(AppLanguage.ENGLISH, new I18n(preferences).language());
    }
    @Test void languageChangePublishesUpdatedCopyToListeners() {
        var i18n = new I18n(new MemoryPreferences()); List<String> observed = new ArrayList<>();
        i18n.languageProperty().addListener((observable, before, after) -> observed.add(i18n.text("nav.settings")));
        i18n.setLanguage(AppLanguage.SIMPLIFIED_CHINESE); i18n.setLanguage(AppLanguage.ENGLISH);
        assertEquals(List.of("设置", "Settings"), observed);
    }
    @Test void resourceKeysAndFormatArgumentsMatchInBothLanguages() {
        var english = ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
        var chinese = ResourceBundle.getBundle("i18n.messages", Locale.SIMPLIFIED_CHINESE);
        assertEquals(english.keySet(), chinese.keySet());
        assertTrue(english.keySet().size() > 400);
        for (String key : english.keySet()) assertEquals(arguments(english.getString(key)), arguments(chinese.getString(key)), key);
    }
    @Test void everyLiteralResourceReferenceExists() throws Exception {
        var messages = ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
        var reference = Pattern.compile("(?:tr|binding)\\(\\s*\"([a-z][a-z0-9_.]+)\"");
        try (var paths = Files.walk(Path.of("src/main/java/com/austin/fileflow"))) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".java")).toList()) {
                var matcher = reference.matcher(Files.readString(path));
                while (matcher.find()) assertTrue(messages.containsKey(matcher.group(1)), path + ": " + matcher.group(1));
            }
        }
    }
    @Test void translatedChoicesKeepTheirStableEnglishIdentifiers() {
        var i18n = new I18n(new MemoryPreferences()); i18n.setLanguage(AppLanguage.SIMPLIFIED_CHINESE);
        assertEquals("名称：A → Z", i18n.text(ChoiceKeys.key("Name A-Z")));
        assertEquals("图片", i18n.text(ChoiceKeys.key("Images")));
        assertEquals("文件", i18n.text(ChoiceKeys.key("Files")));
        assertNull(ChoiceKeys.key("Project proposal.txt"));
        assertNull(ChoiceKeys.key("Home project"));
        assertNull(ChoiceKeys.key("我自己的标签"));
    }
    @Test void namedFileIsInsertedVerbatimIntoAuthoredChineseCopy() {
        var i18n = new I18n(new MemoryPreferences()); i18n.setLanguage(AppLanguage.SIMPLIFIED_CHINESE);
        String name = "Home 100% 中文.jpg";
        assertEquals("收藏：" + name, i18n.text("files.favourite.add_accessible", name));
    }
    private static final class MemoryPreferences extends AbstractPreferences {
        private final Map<String, String> values = new HashMap<>();
        MemoryPreferences() { super(null, ""); }
        protected void putSpi(String key, String value) { values.put(key, value); }
        protected String getSpi(String key) { return values.get(key); }
        protected void removeSpi(String key) { values.remove(key); }
        protected void removeNodeSpi() { values.clear(); }
        protected String[] keysSpi() { return values.keySet().toArray(String[]::new); }
        protected String[] childrenNamesSpi() { return new String[0]; }
        protected AbstractPreferences childSpi(String name) { return new MemoryPreferences(); }
        protected void syncSpi() { }
        protected void flushSpi() { }
    }
}
