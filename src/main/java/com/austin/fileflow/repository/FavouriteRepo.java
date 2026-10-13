package com.austin.fileflow.repository;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

import com.austin.fileflow.database.DatabaseManager;

public class FavouriteRepo {
    
    private final DatabaseManager databaseManager;

    public FavouriteRepo(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void add(Path filePath) throws SQLException {
        String sql = """
                INSERT INTO favourites(file_path)
                VALUES (?)
                """;

        try (Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, filePath.toString());
                statement.executeUpdate();
            }
    }

    public void remove(Path filePath) throws SQLException {
        String sql = """
                DELETE FROM favourites
                WHERE file_path = ?
                """;

        try (Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, filePath.toString());
                statement.executeUpdate();
            }
    }

    public boolean isFavourite(Path filePath) throws SQLException {
        String sql = """
                SELECT file_path
                FROM favourites
                WHERE file_path = ?
                """;

        try (Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, filePath.toString());

                var resultSet = statement.executeQuery();

                return resultSet.next();
            }
    }

    public List<Path> getAll() throws SQLException {
        String sql = """
                SELECT file_path
                FROM favourites
                ORDER BY file_path
                """;

        List<Path> favouriotes = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

                var resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String filePath = resultSet.getString("file_path");
                    favouriotes.add(Path.of(filePath));
                }
            }

        return favouriotes;
    }
}
