package com.austin.fileflow.update;

import java.util.Properties;

public final class BuildInfo {
    private BuildInfo() { }
    public static final String VERSION = loadVersion();
    private static String loadVersion() {
        Properties properties = new Properties();
        try (var input = BuildInfo.class.getResourceAsStream("/app-version.properties")) {
            if (input != null) properties.load(input);
            String version = properties.getProperty("app.version", "");
            SemanticVersion.parse(version); return version;
        } catch (Exception developmentBuild) { return "1.0.0-SNAPSHOT"; }
    }
}
