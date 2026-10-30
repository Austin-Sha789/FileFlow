package com.austin.fileflow.update;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SemanticVersionTest {
    @Test void numericVersionsDoNotUseLexicographicOrdering() {
        assertTrue(SemanticVersion.parse("v1.10.0").compareTo(SemanticVersion.parse("1.9.9")) > 0);
        assertTrue(SemanticVersion.parse("2.0.0").compareTo(SemanticVersion.parse("1.99.99")) > 0);
        assertEquals(0, SemanticVersion.parse("1.0").compareTo(SemanticVersion.parse("1.0.0+build.7")));
    }
    @Test void prereleasePrecedenceFollowsSemverRules() {
        List<String> versions = List.of("1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta", "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0");
        for (int i = 0; i < versions.size() - 1; i++) assertTrue(SemanticVersion.parse(versions.get(i)).compareTo(SemanticVersion.parse(versions.get(i + 1))) < 0);
        assertTrue(SemanticVersion.parse("1.0.0").compareTo(SemanticVersion.parse("1.0.0-SNAPSHOT")) > 0);
    }
    @Test void malformedVersionsAreRejected() {
        for (String value : List.of("latest", "../1.0.0", "1", "01.0.0", "1.0.0-alpha..1", "1.0.0-01", "1.0.0+..")) assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse(value));
    }
    @Test void packagedBuildVersionIsFilteredAndComparable() throws Exception {
        java.util.Properties properties = new java.util.Properties();
        try (var input = BuildInfo.class.getResourceAsStream("/app-version.properties")) { assertNotNull(input); properties.load(input); }
        assertFalse(properties.getProperty("app.version").contains("${"));
        assertEquals(properties.getProperty("app.version"), BuildInfo.VERSION); assertNotNull(SemanticVersion.parse(BuildInfo.VERSION));
    }
}
