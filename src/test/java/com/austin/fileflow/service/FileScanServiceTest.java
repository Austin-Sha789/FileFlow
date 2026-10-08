package com.austin.fileflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.austin.fileflow.model.FileItem;
import com.austin.fileflow.model.FileType;
import com.austin.fileflow.model.ScanResult;

public class FileScanServiceTest {
    
    @TempDir 
    Path tempDir;

    @Test 
    void scanReturnsFilesAndDirectories() throws IOException {
        Path report = Files.createFile(tempDir.resolve("report.pdf"));

        Path photos = Files.createDirectory(tempDir.resolve("photos"));

        Path photo = Files.createFile(photos.resolve("cat.jpg"));

        FileScanService service = new FileScanService();

        ScanResult result = service.scan(tempDir);
        List<FileItem> items = result.getItems();

        assertEquals(4, items.size());

        assertTrue(
            items.stream().anyMatch(item -> item.getName().equals("report.pdf"))
        );

        assertTrue(
            items.stream().anyMatch(item -> item.getName().equals("photos"))
        );

        assertTrue(
            items.stream().anyMatch(item -> item.getName().equals("cat.jpg"))
        );

        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    void scanCreatesCorrectFileMetadata() throws IOException {
        Path report = Files.createFile(tempDir.resolve("report.pdf"));

        Path photos = Files.createDirectory(tempDir.resolve("photos"));

        Path photo = Files.createFile(photos.resolve("cat.jpg"));

        FileScanService service = new FileScanService();

        ScanResult result = service.scan(tempDir);
        List<FileItem> items = result.getItems();

        FileItem reportItem = items.stream()
                .filter(item -> item.getName().equals("report.pdf"))
                .findFirst()
                .orElseThrow();

        FileItem photoItem = items.stream()
                .filter(item -> item.getName().equals("cat.jpg"))
                .findFirst()
                .orElseThrow();

        FileItem photosItem = items.stream()
                .filter(item -> item.getName().equals("photos"))
                .findFirst()
                .orElseThrow();

        assertEquals(FileType.DOCUMENT, reportItem.getType());
        assertEquals("pdf", reportItem.getExtension());

        assertEquals(FileType.IMAGE, photoItem.getType());
        assertEquals("jpg", photoItem.getExtension());

        assertTrue(photosItem.isDirectory());
    }
}
