package com.austin.fileflow.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;

import com.austin.fileflow.database.DatabaseManager;

public class TagRepo {
    
    private final DatabaseManager databaseManager;

    public TagRepo(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void add(String name) throws SQLException {
        String sql = """
                INSERT INTO tags(name)
                VALUES (?)
                """;

        try (Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, name);
                statement.executeUpdate();
            }
    }

    public List<String> getAll() throws SQLException {
        String sql = """
                SELECT name
                FROM tags
                ORDER BY name
                """;

        List<String> tags = new ArrayList<>();
        
        try (Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

                var resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    tags.add(name);
                }
            }

        return tags;
    }

    public void addTagToFile(Path filePath, String tagName) throws SQLException {
        String findTagsSql = """
                SELECT id
                FROM tags
                WHERE name = ?
                """;

        String addFileTagSql = """
                INSERT OR IGNORE INTO file_tags(file_path, tag_id)
                VALUES (?, ?)
                """;

        try (Connection connection = databaseManager.getConnection();
            PreparedStatement findTagStatement = connection.prepareStatement(findTagsSql)) {

                findTagStatement.setString(1, tagName);

                var resultSet = findTagStatement.executeQuery();

                if (!resultSet.next()) {
                    throw new SQLException("Tag does not exist: " + tagName);
                }

                int tagId = resultSet.getInt("id");

                try (PreparedStatement addFileTagStatement = connection.prepareStatement(addFileTagSql)) {

                    addFileTagStatement.setString(1, filePath.toString());
                    addFileTagStatement.setInt(2, tagId);

                    addFileTagStatement.executeUpdate();
                }
            }
    }

    public List<String> getTagsForFile(Path filePath) throws SQLException {
        String sql = """
                SELECT tags.name
                FROM file_tags
                JOIN tags ON file_tags.tag_id = tags.id
                WHERE file_tags.file_path = ?
                ORDER BY tags.name
                """;

        List<String> tags = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, filePath.toString());

                var resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String tagName = resultSet.getString("name");
                    tags.add(tagName);
                }

            }

        return tags;
    }

    public void removeTagFromFile(Path filePath, String tagName) throws SQLException {
        String sql = """
                DELETE FROM file_tags
                WHERE file_path = ?
                AND tag_id = (
                    SELECT id
                    FROM tags
                    WHERE name = ?
                )
                """;

        try (Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, filePath.toString());
                statement.setString(2, tagName);

                statement.executeUpdate();
            }
    }

    public void deleteTag(String tagName) throws SQLException {
        String deleteFileTagsSql = """
                DELETE FROM file_tags
                WHERE tag_id = (
                    SELECT id
                    FROM tags
                    WHERE name = ?
                )
                """;

        String deleteTagSql = """
                DELETE FROM tags
                WHERE name = ?
                """;

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement deleteFileTagsStatement = connection.prepareStatement(deleteTagSql);
                PreparedStatement deleteTagStatement = connection.prepareStatement(deleteTagSql)) {

                deleteFileTagsStatement.setString(1, tagName);
                deleteFileTagsStatement.executeUpdate();

                deleteTagStatement.setString(1, tagName);
                deleteTagStatement.executeUpdate();

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
            
    }

    public List<Path> getFilesForTag(String tagName) throws SQLException {
        String sql = """
                SELECT file_tags.file_path
                FROM file_tags
                JOIN tags ON file_tags.tag_id = tags.id
                WHERE tags.name = ?
                ORDER BY file_tags.file_path
                """;

        List<Path> files = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, tagName);

            var resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String filePath = resultSet.getString("file_path");
                files.add(Path.of(filePath));
            }
        }

        return files;
    }
}
