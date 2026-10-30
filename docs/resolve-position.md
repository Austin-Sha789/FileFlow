# Preserve result positions after Resolve

Duplicate and similar-image cards carry their normalized member paths as stable identities. Before a Duplicates page refresh, ResultScrollPosition records the first visible group, its pixel offset and exact-group expansion states. After rebuilding the page it restores those states and locates the same members. If that group no longer exists, the next surviving group takes its place, with a previous-group fallback at the end of the list. The restored offset is clamped to the available scroll range.

This works through both the immediate post-Resolve refresh and the subsequent asynchronous metadata refresh. It does not reuse an obsolete percentage of the old content height. New analysis or navigation is not forced to a saved historical position.

Workspace refresh now uses the same reconciliation as a single-root refresh: removal-only changes preserve remaining exact/similar matches; added or modified relevant files still invalidate stale analysis. Resolving everything can naturally leave an empty list, and short lists clamp to the available viewport.

Integration coverage checks stable group/pixel offsets across two rebuilds, expansion state, removal of the current similar group, last exact-group removal, and a real asynchronous workspace metadata refresh following a temporary-file deletion.
