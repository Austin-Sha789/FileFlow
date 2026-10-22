package com.austin.fileflow.repository;

import com.austin.fileflow.database.DatabaseManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.List;
import java.util.Map;

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

    @Test
    void deleteTagRemovesTagAndAllFileRelationships() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        TagRepo tagRepo = new TagRepo(databaseManager);

        Path firstFile = Path.of("C:/test/report.pdf");
        Path secondFile = Path.of("C:/test/photo.jpg");

        tagRepo.add("Work");

        tagRepo.addTagToFile(firstFile, "Work");
        tagRepo.addTagToFile(secondFile, "Work");

        tagRepo.deleteTag("Work");

        List<String> firstFileTags = tagRepo.getTagsForFile(firstFile);
        List<String> secondFileTags = tagRepo.getTagsForFile(secondFile);
        List<String> allTags = tagRepo.getAll();

        assertTrue(firstFileTags.isEmpty());
        assertTrue(secondFileTags.isEmpty());
        assertTrue(!allTags.contains("Work"));
    }

    @Test
    void addTagToFileIgnoresDuplicateRelationship() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        TagRepo tagRepo = new TagRepo(databaseManager);

        Path filePath = Path.of("C:/test/report.pdf");

        tagRepo.add("Work");

        tagRepo.addTagToFile(filePath, "Work");
        tagRepo.addTagToFile(filePath, "Work");

        List<String> tags = tagRepo.getTagsForFile(filePath);

        assertEquals(1, tags.size());
        assertTrue(tags.contains("Work"));
    }

    @Test
    void getFilesForTagReturnsOnlyFilesWithThatTag() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        TagRepo tagRepo = new TagRepo(databaseManager);

        Path firstFile = Path.of("C:/test/report.pdf");
        Path secondFile = Path.of("C:/test/notes.txt");
        Path thirdFile = Path.of("C:/test/photo.jpg");

        tagRepo.add("Work");
        tagRepo.add("Photo");

        tagRepo.addTagToFile(firstFile, "Work");
        tagRepo.addTagToFile(secondFile, "Work");
        tagRepo.addTagToFile(thirdFile, "Photo");

        List<Path> workFiles = tagRepo.getFilesForTag("Work");

        assertEquals(2, workFiles.size());
        assertTrue(workFiles.contains(firstFile));
        assertTrue(workFiles.contains(secondFile));
        assertTrue(!workFiles.contains(thirdFile));
    }

    @Test
    void getAllFileTagsReturnsAllFileTagMappings() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        TagRepo tagRepo = new TagRepo(databaseManager);

        Path firstFile = Path.of("C:/test/report.pdf");
        Path secondFile = Path.of("C:/test/photo.jpg");

        tagRepo.add("Work");
        tagRepo.add("Important");
        tagRepo.add("Photo");

        tagRepo.addTagToFile(firstFile, "Work");
        tagRepo.addTagToFile(firstFile, "Important");
        tagRepo.addTagToFile(secondFile, "Photo");

        Map<Path, List<String>> fileTags = tagRepo.getAllFileTags();

        assertEquals(2, fileTags.size());

        assertTrue(fileTags.containsKey(firstFile));
        assertTrue(fileTags.containsKey(secondFile));

        assertEquals(2, fileTags.get(firstFile).size());
        assertTrue(fileTags.get(firstFile).contains("Work"));
        assertTrue(fileTags.get(firstFile).contains("Important"));

        assertEquals(1, fileTags.get(secondFile).size());
        assertTrue(fileTags.get(secondFile).contains("Photo"));
    }
}

