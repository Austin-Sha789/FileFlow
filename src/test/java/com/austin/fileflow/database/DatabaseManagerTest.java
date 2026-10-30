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
    

    @Test void userDataDirectoriesAreIndependentOfTheInstallFolder() {
        org.junit.jupiter.api.Assertions.assertEquals(Path.of("home", "Library", "Application Support", "FileFlow"), DatabaseManager.dataDirectory("Mac OS X", "home", null, null));
        org.junit.jupiter.api.Assertions.assertEquals(Path.of("local", "FileFlow"), DatabaseManager.dataDirectory("Windows 11", "home", "local", null));
        org.junit.jupiter.api.Assertions.assertEquals(Path.of("home", "AppData", "Local", "FileFlow"), DatabaseManager.dataDirectory("Windows 11", "home", null, null));
    }
    @Test void legacyDatabaseIsCopiedConsistentlyWithoutRemovingOrOverwritingData() throws Exception {
        Path legacy=tempDir.resolve("old.db"), target=tempDir.resolve("user's data/fileflow.db");
        var old=new DatabaseManager("jdbc:sqlite:"+legacy);old.initialiseDatabase();
        try(var connection=old.getConnection();var statement=connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");statement.execute("INSERT INTO favourites VALUES ('saved-photo.jpg')");
            DatabaseManager.prepareManagedDatabase(target,legacy);
        }
        org.junit.jupiter.api.Assertions.assertTrue(java.nio.file.Files.exists(legacy));
        try(var connection=new DatabaseManager("jdbc:sqlite:"+target).getConnection();var statement=connection.createStatement();var result=statement.executeQuery("SELECT file_path FROM favourites")) {
            assertTrue(result.next());org.junit.jupiter.api.Assertions.assertEquals("saved-photo.jpg",result.getString(1));
        }
        try(var connection=old.getConnection();var statement=connection.createStatement()){statement.execute("DELETE FROM favourites");}
        DatabaseManager.prepareManagedDatabase(target,legacy);
        try(var connection=new DatabaseManager("jdbc:sqlite:"+target).getConnection();var statement=connection.createStatement();var result=statement.executeQuery("SELECT count(*) FROM favourites")) {
            assertTrue(result.next());org.junit.jupiter.api.Assertions.assertEquals(1,result.getInt(1));
        }
    }
}
