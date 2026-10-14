package com.austin.fileflow.repository;

import com.austin.fileflow.database.DatabaseManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.List;

import javax.swing.text.html.HTML.Tag;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TagRepoTest {

    @TempDir 
    Path tempDir;

    @Test 
    void addStoresTagInDatabase() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        TagRepo tagRepo = new TagRepo(databaseManager);

        tagRepo.add("Work");

        String sql = """
                SELECT name
                FROM tags
                WHERE name = ?
                """;

        try (Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, "Work");

                var resultSet = statement.executeQuery();

                assertTrue(resultSet.next());
            }
    }

    @Test 
    void getAllReturnsAllTags() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        TagRepo tagRepo = new TagRepo(databaseManager);

        tagRepo.add("Work");
        tagRepo.add("Important");

        List<String> tags = tagRepo.getAll();

        assertEquals(2, tags.size());
        assertTrue(tags.contains("Work"));
        assertTrue(tags.contains("Important"));
    }
    

    @Test 
    void addTagToFileCreatesFileTagRelationship() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        TagRepo tagRepo = new TagRepo(databaseManager);

        Path filePath = Path.of("C:/test/report.pdf");

        tagRepo.add("Work");
        tagRepo.addTagToFile(filePath, "Work");

        String sql = """
                SELECT file_tags.file_path
                FROM file_tags
                JOIN tags ON file_tags.tag_id = tags.id
                WHERE file_tags.file_path = ?
                AND tags.name = ?
                """;

        try (Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, filePath.toString());
                statement.setString(2, "Work");

                var resultSet = statement.executeQuery();

                assertTrue(resultSet.next());
            }
    }

    @Test 
    void getTagsForFileReturnsAllTagsForFile() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        TagRepo tagRepo = new TagRepo(databaseManager);

        Path filePath = Path.of("C:/test/report.pdf");

        tagRepo.add("Work");
        tagRepo.add("Important");

        tagRepo.addTagToFile(filePath, "Work");
        tagRepo.addTagToFile(filePath, "Important");

        List<String> tags = tagRepo.getTagsForFile(filePath);

        assertEquals(2, tags.size());
        assertTrue(tags.contains("Work"));
        assertTrue(tags.contains("Important"));
    }

    @Test 
    void removeTagFromFileDeletesRelationship() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        TagRepo tagRepo = new TagRepo(databaseManager);

        Path filePath = Path.of("C:/test/report.pdf");

        tagRepo.add("Work");
        tagRepo.addTagToFile(filePath, "Work");

        tagRepo.removeTagFromFile(filePath, "Work");

        List<String> tags = tagRepo.getTagsForFile(filePath);

        assertTrue(tags.isEmpty());
    }
}

