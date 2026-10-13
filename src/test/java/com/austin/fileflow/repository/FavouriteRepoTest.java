package com.austin.fileflow.repository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.austin.fileflow.database.DatabaseManager;

public class FavouriteRepoTest {

    @TempDir
    Path tempDir;
    
    @Test
    void addStoresFavouritePath() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager("jdbc:sqlite:" + testDatabase);
        databaseManager.initialiseDatabase();

        FavouriteRepo favouriteRepo = new FavouriteRepo(databaseManager);

        Path filePath = Path.of("C:/test/photo.jpg");

        favouriteRepo.add(filePath);

        String sql = """
                SELECT file_path
                FROM favourites
                WHERE file_path = ?
                """;

        try (Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, filePath.toString());

                var resultSet = statement.executeQuery();

                System.out.println(filePath.toString());
                assertTrue(resultSet.next());
            }
    }

    @Test 
    void removeDeletesFavouritePath() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        FavouriteRepo favouriteRepo = new FavouriteRepo(databaseManager);

        Path filePath = Path.of("C:/test/photo.jpg");

        favouriteRepo.add(filePath);
        favouriteRepo.remove(filePath);

        String sql = """
                SELECT file_path
                FROM favourites
                WHERE file_path = ?
                """;

        try (Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, filePath.toString());

                var resultSet = statement.executeQuery();

            assertTrue(!resultSet.next());
            }
    }

    @Test 
    void isFavouriteReturnsTrueWhenPathExists() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        FavouriteRepo favouriteRepo = new FavouriteRepo(databaseManager);

        Path filePath = Path.of("C:/test/photo.jpg");

        favouriteRepo.add(filePath);

        assertTrue(favouriteRepo.isFavourite(filePath));
    }

    @Test
    void isFavouriteReturnsFalseWhenPathDoesNotExist() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        FavouriteRepo favouriteRepo = new FavouriteRepo(databaseManager);

        Path filePath = Path.of("C:/test/photo.jpg");

        assertTrue(!favouriteRepo.isFavourite(filePath));
    }

    @Test 
    void getAllReturnsAllFavouritePaths() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        FavouriteRepo favouriteRepo = new FavouriteRepo(databaseManager);

        Path firstPath = Path.of("C:/test/a.txt");
        Path secPath = Path.of("C:/test/photo.jpg");

        favouriteRepo.add(firstPath);
        favouriteRepo.add(secPath);

        List<Path> favourites = favouriteRepo.getAll();

        assertEquals(2, favourites.size());
        assertTrue(favourites.contains(firstPath));
        assertTrue(favourites.contains(secPath));
    }


}

