# Files presentation and temporary Home analysis

## Files views

The Files page offers List, Grid, and Details only views, plus Compact,
Comfortable, and Large sizes. These choices are stored locally in preferences.
Changing presentation reuses the current filtered and sorted item collection;
it does not scan again or clear the current folder, search, or bulk selection.

List and Details only use the existing virtualized table. Details only does
not create preview nodes and shows type, size, and modified time. Grid uses a
virtualized ListView of row indices, creating cards and asynchronous previews
only for visible rows. Column count follows available width and item size.
Grid supports opening files/folders, quick information, selection, favourites,
adding/removing tags, and the existing file context menu.

## Home analysis shortcuts

Large Files, Recently Modified, and Find Duplicates first open a temporary scope
picker. Users can add multiple files and folders, or drop them into the picker.
Cancel leaves the current results unchanged. Analyse scans only those paths in
a background task and opens Dashboard or Duplicates as appropriate. Dashboard
puts the requested large-file/recent-file section near the top and expands it.

The chosen scope is shown on results pages. It is not added to the saved
workspace. Overlapping selections are deduplicated. Selecting an individual
file never scans its siblings. Manual refresh and post-action refresh continue
to use the same selected paths. If a new selection cannot be read at all, the
previous results remain available. Partial access failures are reported.

## Validation

- Full Maven suite: 126 tests, no failures or errors, 2 environment-dependent skips.
- JavaFX checks on Windows with more than 10,000 items: list/grid virtualization,
  deep scrolling, all list sizes, selection preservation, saved view choice,
  search preservation, and no thumbnail nodes in Details only.
- JavaFX selected-scope checks: exact selected files, Dashboard navigation and
  expanded section, no workspace additions, scoped refresh, and preservation
  of previous results after an unreadable selection.
- macOS visual validation remains to be performed on a Mac.

These changes require a future release; existing v1.0.0 installers are unchanged.
