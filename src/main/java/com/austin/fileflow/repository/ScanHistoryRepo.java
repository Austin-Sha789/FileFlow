package com.austin.fileflow.repository;

import com.austin.fileflow.database.DatabaseManager;
import com.austin.fileflow.model.ScanHistoryEntry;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScanHistoryRepo {

    private final DatabaseManager databaseManager;
    
    public ScanHistoryRepo(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void add(
        Path folderPath,
        LocalDateTime scannedAt,
        long fileCount,
        long totalBytes
    ) throws SQLException {

        String deleteSql =
                "DELETE FROM scan_history "
                + "WHERE folder_path = ?";

        String sql = """
                INSERT INTO scan_history (
                    folder_path,
                    scanned_at,
                    file_count,
                    total_bytes
                )
                VALUES(?, ?, ?, ?)
                """;

        try (Connection connection =
            databaseManager.getConnection()) {

            connection.setAutoCommit(false);

            try {

                try (PreparedStatement deleteStatement =
                        connection.prepareStatement(deleteSql)) {

                    deleteStatement.setString(
                            1,
                            folderPath.toString()
                    );

                    deleteStatement.executeUpdate();
                }

                try (PreparedStatement statement =
                        connection.prepareStatement(sql)) {

                    statement.setString(
                            1,
                            folderPath.toString()
                    );

                    statement.setString(
                            2,
                            scannedAt.toString()
                    );

                    statement.setLong(
                            3,
                            fileCount
                    );

                    statement.setLong(
                            4,
                            totalBytes
                    );

                    statement.executeUpdate();
                }

                connection.commit();

            } catch (SQLException exception) {
                
                connection.rollback();

                throw exception;
            }
        }
    }

    public List<ScanHistoryEntry> getAll() throws SQLException {

        String sql = """
                SELECT
                    id,
                    folder_path,
                    scanned_at,
                    file_count,
                    total_bytes
                FROM scan_history
                ORDER BY scanned_at DESC, id DESC
                """;

        List<ScanHistoryEntry> history = new ArrayList<>();

        try (
            Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
        ) {

            while (resultSet.next()) {

                ScanHistoryEntry entry = new ScanHistoryEntry(
                        resultSet.getLong("id"),
                        Path.of(resultSet.getString("folder_path")),
                        LocalDateTime.parse(resultSet.getString("scanned_at")),
                        resultSet.getLong("file_count"),
                        resultSet.getLong("total_bytes")
                );

                history.add(entry);
            }
        }

        return List.copyOf(history);
    }

    public void remove(long id) throws SQLException {

        String sql = """
                DELETE FROM scan_history
                WHERE id = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    id
            );

            statement.executeUpdate();
        }
    }

    public void clearAll() throws SQLException {

        String sql =
                "DELETE FROM scan_history";

        try (Connection connection =
                databaseManager.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(sql)) {

            statement.executeUpdate();
        }
    }
    
}
