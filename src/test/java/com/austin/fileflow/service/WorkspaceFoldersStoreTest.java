package com.austin.fileflow.service;

import com.austin.fileflow.support.MemoryPreferencesFactory;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceFoldersStoreTest {
    @Test void folderOrderAndNestedEntriesSurviveAStoreReload() {
        var preferences = new MemoryPreferencesFactory().userRoot();
        var store = new WorkspaceFoldersStore(preferences);
        List<Path> paths = List.of(Path.of("Photos").toAbsolutePath(), Path.of("Photos", "Trips").toAbsolutePath(), Path.of("Projects").toAbsolutePath());
        assertTrue(store.save(paths));
        assertEquals(paths, new WorkspaceFoldersStore(preferences).load());
    }
    @Test void removingFoldersAndSavingEmptyDoesNotResurrectOldEntries() {
        var preferences = new MemoryPreferencesFactory().userRoot();
        var store = new WorkspaceFoldersStore(preferences);
        store.save(List.of(Path.of("One"), Path.of("Two")));
        store.save(List.of(Path.of("Two")));
        assertEquals(List.of(Path.of("Two").toAbsolutePath()), store.load());
        store.save(List.of()); assertTrue(new WorkspaceFoldersStore(preferences).load().isEmpty());
        assertNull(preferences.get("folder.0", null)); assertNull(preferences.get("folder.1", null));
    }
    @Test void unavailablePreferencesLeaveTheApplicationUsable() {
        var store = new WorkspaceFoldersStore(null);
        assertTrue(store.load().isEmpty()); assertFalse(store.save(List.of(Path.of("One"))));
    }
    @Test void savedFolderLibrariesKeepParentAndChildWhileBatchPlansStillDeduplicate() {
        var library = new ScanWorkspace(false);
        Path parent = Path.of("Photos").toAbsolutePath(), child = parent.resolve("Trips");
        assertTrue(library.addRoot(parent)); assertTrue(library.addRoot(child)); assertFalse(library.addRoot(child.resolve(".")));
        assertEquals(List.of(parent, child), library.getRoots());
        assertEquals(List.of(parent), ScanRootPlanner.plan(library.getRoots()));
    }
}
