package com.austin.fileflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import com.austin.fileflow.model.DuplicateGroup;
import com.austin.fileflow.model.FileItem;
import com.austin.fileflow.model.FileType;

class DuplicateServiceTest {

    private final DuplicateService duplicateService = new DuplicateService();

    @TempDir 
    Path tempDir;
    
    @Test 
    void findsOnlyFilesWithMatchingSizes() {

        FileItem first = createFile(
            "first.txt",
            100L,
            false
        );

        FileItem second = createFile(
            "second.txt",
            100L,
            false
        );

        FileItem unique = createFile(
                "unique.txt",
                200L,
                false
        );

        FileItem empty = createFile(
                "empty.txt",
                0L,
                false
        );

        FileItem folder = createFile(
                "folder",
                100L,
                true
        );

        Map<Long, List<FileItem>> result = duplicateService.findSizeCandidates(
            List.of(
                first,
                second,
                unique,
                empty,
                folder
            )
        );

        assertEquals(1, result.size());

        assertTrue(result.containsKey(100L));

        assertEquals(
            2,
            result.get(100L).size()
        );

        assertFalse(result.containsKey(200L));
        assertFalse(result.containsKey(0L));
    }

    private FileItem createFile(
            String name,
            long size,
            boolean directory
    ) {
        return new FileItem(
                Path.of(name),
                name,
                "",
                size,
                LocalDateTime.now(),
                LocalDateTime.now(),
                FileType.OTHER,
                directory,
                false,
                Set.of()
        );
    }

    private FileItem createFile(
            Path path,
            long size
    ) {
        return new FileItem(
                path,
                path.getFileName().toString(),
                "txt",
                size,
                LocalDateTime.now(),
                LocalDateTime.now(),
                FileType.DOCUMENT,
                false,
                false,
                Set.of()
        );
    }

    @Test
    void findsDuplicatesByFileContent() throws IOException {

        Path firstPath = tempDir.resolve("first.txt");
        Path secondPath = tempDir.resolve("second.txt");
        Path differentPath = tempDir.resolve("different.txt");

        Files.writeString(firstPath, "ABCDEF");
        Files.writeString(secondPath, "ABCDEF");
        Files.writeString(differentPath, "UVWXYZ");

        FileItem first = createFile(
                firstPath,
                Files.size(firstPath)
        );

        FileItem second = createFile(
                secondPath,
                Files.size(secondPath)
        );

        FileItem different = createFile(
                differentPath,
                Files.size(differentPath)
        );

        List<DuplicateGroup> result =
                duplicateService.findDuplicates(
                        List.of(
                                first,
                                second,
                                different
                        )
                );

        assertEquals(1, result.size());

        DuplicateGroup group = result.get(0);

        assertEquals(2, group.getFiles().size());

        assertTrue(group.getFiles().contains(first));
        assertTrue(group.getFiles().contains(second));
        assertFalse(group.getFiles().contains(different));

        assertEquals(
                Files.size(firstPath),
                group.getReclaimableBytes()
        );
    }

    @Test
    void continuesWhenCandidateFileCannotBeRead()
            throws IOException {

        Path firstPath =
                tempDir.resolve("first.txt");

        Path secondPath =
                tempDir.resolve("second.txt");

        Path missingPath =
                tempDir.resolve("missing.txt");

        Files.writeString(
                firstPath,
                "ABCDEF"
        );

        Files.writeString(
                secondPath,
                "ABCDEF"
        );

        FileItem first = createFile(
                firstPath,
                Files.size(firstPath)
        );

        FileItem second = createFile(
                secondPath,
                Files.size(secondPath)
        );

        FileItem missing = createFile(
                missingPath,
                6L
        );

        List<DuplicateGroup> result =
                duplicateService.findDuplicates(
                        List.of(
                                first,
                                second,
                                missing
                        )
                );

        assertEquals(
                1,
                result.size()
        );

        DuplicateGroup group =
                result.get(0);

        assertEquals(
                2,
                group.getFiles().size()
        );

        assertTrue(
                group.getFiles().contains(first)
        );

        assertTrue(
                group.getFiles().contains(second)
        );

        assertFalse(
                group.getFiles().contains(missing)
        );
    }

    @Test
    void stopsWhenDuplicateScanIsCancelled()
            throws IOException {

        Path firstPath =
                tempDir.resolve("first.txt");

        Path secondPath =
                tempDir.resolve("second.txt");

        Files.writeString(
                firstPath,
                "ABCDEF"
        );

        Files.writeString(
                secondPath,
                "ABCDEF"
        );

        FileItem first = createFile(
                firstPath,
                Files.size(firstPath)
        );

        FileItem second = createFile(
                secondPath,
                Files.size(secondPath)
        );

        AtomicInteger cancellationChecks =
                new AtomicInteger();

        assertThrows(
                CancellationException.class,
                () -> duplicateService.findDuplicates(
                        List.of(
                                first,
                                second
                        ),
                        () ->
                                cancellationChecks
                                        .incrementAndGet() > 1
                )
        );
    }

    @Test
    void reportsDuplicateScanProgress()
            throws IOException {

        Path firstPath =
                tempDir.resolve("first.txt");

        Path secondPath =
                tempDir.resolve("second.txt");

        Files.writeString(
                firstPath,
                "ABCDEF"
        );

        Files.writeString(
                secondPath,
                "ABCDEF"
        );

        FileItem first = createFile(
                firstPath,
                Files.size(firstPath)
        );

        FileItem second = createFile(
                secondPath,
                Files.size(secondPath)
        );

        List<String> progressUpdates =
                new ArrayList<>();

        duplicateService.findDuplicates(
                List.of(
                        first,
                        second
                ),
                () -> false,
                (processed, total) ->
                        progressUpdates.add(
                                processed + "/" + total
                        )
        );

        assertEquals(
                "0/2",
                progressUpdates.get(0)
        );

        assertEquals(
                "2/2",
                progressUpdates.get(
                        progressUpdates.size() - 1
                )
        );
    }
    
}
