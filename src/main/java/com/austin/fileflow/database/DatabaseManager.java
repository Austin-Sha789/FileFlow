package com.austin.fileflow.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.nio.file.Path;

public class DatabaseManager {
    
    private final String databaseUrl;
    private Path managedPath;

    public DatabaseManager() {
        this("jdbc:sqlite:" + defaultDatabasePath());
        managedPath = defaultDatabasePath();
    }

    public static Path defaultDatabasePath() {
        String override = System.getProperty("fileflow.dataDir");
        if (override != null && !override.isBlank()) return Path.of(override).toAbsolutePath().normalize().resolve("fileflow.db");
        return dataDirectory(System.getProperty("os.name", ""), System.getProperty("user.home"),
                System.getenv("LOCALAPPDATA"), System.getenv("XDG_DATA_HOME")).resolve("fileflow.db");
    }

    static Path dataDirectory(String os, String home, String local, String xdg) {
        os = os.toLowerCase(java.util.Locale.ROOT);
        if (os.contains("mac")) return Path.of(home, "Library", "Application Support", "FileFlow");
        if (os.contains("win")) return (local == null || local.isBlank() ? Path.of(home, "AppData", "Local") : Path.of(local)).resolve("FileFlow");
        return (xdg == null || xdg.isBlank() ? Path.of(home, ".local", "share") : Path.of(xdg)).resolve("fileflow");
    }

    /** SQLite creates a consistent copy even when a legacy DB is using WAL. Never remove the original. */
    static void prepareManagedDatabase(Path destination, Path legacy) throws SQLException {
        try {
            java.nio.file.Files.createDirectories(destination.toAbsolutePath().getParent());
            if (!java.nio.file.Files.exists(destination) && !destination.toAbsolutePath().normalize().equals(legacy.toAbsolutePath().normalize())
                    && java.nio.file.Files.isRegularFile(legacy, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                try (Connection old = DriverManager.getConnection("jdbc:sqlite:" + legacy); Statement statement = old.createStatement()) {
                    statement.execute("VACUUM INTO '" + destination.toAbsolutePath().toString().replace("'", "''") + "'");
                }
            }
        } catch (java.io.IOException error) { throw new SQLException("Cannot prepare FileFlow user data directory", error); }
    }

    public DatabaseManager(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (managedPath != null) prepareManagedDatabase(managedPath, System.getProperty("fileflow.dataDir") == null ? Path.of("fileflow.db").toAbsolutePath() : managedPath);
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
                    file_path TEXT NOT NULL,
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

            if (!columnExists(
                    connection,
                    "scan_history",
                    "file_count"
            )) {
                statement.execute(
                        """
                        ALTER TABLE scan_history
                        ADD COLUMN file_count INTEGER NOT NULL DEFAULT 0
                        """
                );
            }

            if (!columnExists(
                    connection,
                    "scan_history",
                    "total_bytes"
            )) {
                statement.execute(
                        """
                        ALTER TABLE scan_history
                        ADD COLUMN total_bytes INTEGER NOT NULL DEFAULT 0
                        """
                );
            }
        }
    }

    private boolean columnExists(
            Connection connection,
            String tableName,
            String columnName
    ) throws SQLException {

        String sql =
                "PRAGMA table_info(" + tableName + ")";

        try (
                Statement statement =
                        connection.createStatement();

                ResultSet resultSet =
                        statement.executeQuery(sql)
        ) {

            while (resultSet.next()) {

                if (columnName.equals(
                        resultSet.getString("name")
                )) {
                    return true;
                }
            }
        }

        return false;
    }
}   

