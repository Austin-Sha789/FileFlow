package com.austin.fileflow.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    
    private static final String DATABASE_URL = "jdbc:sqlite:fileflow.db";
    
    private final String databaseUrl;

    public DatabaseManager() {
        this(DATABASE_URL);
    }

    public DatabaseManager(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }

    public void initialiseDatabase() throws SQLException {
        String createScanHistoryTable = """
                CREATE TABLE IF NOT EXISTS scan_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    folder_path TEXT NOT NULL,
                    scanned_at TEXT NOT NULL,
                    file_count INTEGER NOT NULL,
                    total_bytes INTEGER NOT NULL
                )
                """;

        String createFavouritesTable = """
                CREATE TABLE IF NOT EXISTS favourites (
                    file_path TEXT NOT NULL UNIQUE
                )
                """;

        String createTagsTable = """
                CREATE TABLE IF NOT EXISTS tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                )
                """;

        String createFileTagsTable = """
                CREATE TABLE IF NOT EXISTS file_tags (
                    file_path TEXT NOE NULL,
                    tag_id INTEGER NOT NULL,
                    PRIMARY KEY (file_path, tag_id),
                    FOREIGN KEY (tag_id) REFERENCES tags(id)
                )
                """;

        try (Connection connection = getConnection();
            Statement statement = connection.createStatement()) {

            statement.execute(createScanHistoryTable);
            statement.execute(createFavouritesTable);
            statement.execute(createTagsTable);
            statement.execute(createFileTagsTable);
        }
    }
}   
