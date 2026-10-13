package com.austin.fileflow.database;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DatabaseManagerTest {

    @TempDir 
    Path tempDir;

    @Test 
    void canConnectToDatabase() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        try (Connection connection = databaseManager.getConnection()) {
            assertNotNull(connection);
        }
    }

    @Test 
    void initialiseDatabaseCreateScanHistoryTable() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        try (Connection connection = databaseManager.getConnection();
            Statement statement = connection.createStatement()) {

            var resultSet = statement.executeQuery(
                    "SELECT name FROM sqlite_master " +
                    "WHERE type ='table' AND name = 'scan_history'"
            );

            assertTrue(resultSet.next());
        }

    }

    @Test 
    void initialiseDatabaseCreatesFavouritesTable() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        try (Connection connection = databaseManager.getConnection();
            Statement statement = connection.createStatement()) {
                
                var resultSet = statement.executeQuery(
                    "SELECT name FROM sqlite_master " + 
                    "WHERE type = 'table' AND name = 'favourites'"  
                );

            assertTrue(resultSet.next());
        }
    }

    @Test 
    void initialiseDatabaseCreatesTagsTable() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        try (Connection connection = databaseManager.getConnection();
            Statement statement = connection.createStatement()) {
                
            var resultSet = statement.executeQuery(
                    "SELECT name FROM sqlite_master " + 
                    "WHERE type = 'table' AND name = 'tags'"   
            );

            assertTrue(resultSet.next());
        }
    }

    @Test
    void initialiseDatabaseCreatesFileTagsTable() throws SQLException {
        Path testDatabase = tempDir.resolve("test.db");

        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + testDatabase
        );

        databaseManager.initialiseDatabase();

        try(Connection connection = databaseManager.getConnection();
            Statement statement = connection.createStatement()) {

                var resultSet = statement.executeQuery(
                        "SELECT name FROM sqlite_master " + 
                        "WHERE type = 'table' AND name = 'file_tags'"
                );

                assertTrue(resultSet.next());
            }
    }
    
}
