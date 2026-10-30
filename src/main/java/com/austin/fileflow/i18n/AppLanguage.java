package com.austin.fileflow.i18n;

import java.util.Locale;

public enum AppLanguage {
    ENGLISH("en", Locale.ENGLISH, "English"),
    SIMPLIFIED_CHINESE("zh-CN", Locale.SIMPLIFIED_CHINESE, "简体中文");

    private final String tag;
    private final Locale locale;
    private final String label;
    AppLanguage(String tag, Locale locale, String label) { this.tag = tag; this.locale = locale; this.label = label; }
    public String tag() { return tag; }
    public Locale locale() { return locale; }
    @Override public String toString() { return label; }
    public static AppLanguage fromTag(String tag) {
        for (AppLanguage language : values()) if (language.tag.equalsIgnoreCase(tag)) return language;
        return ENGLISH;
    }
}
