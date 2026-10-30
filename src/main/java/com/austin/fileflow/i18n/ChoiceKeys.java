package com.austin.fileflow.i18n;

final class ChoiceKeys {
    private ChoiceKeys() { }
    static String key(String value) {
        if (value == null) return null;
        return switch (value) {
            case "Home" -> "nav.home";
            case "Dashboard" -> "nav.dashboard";
            case "Files" -> "nav.files";
            case "Favourites" -> "nav.favourites";
            case "Duplicates" -> "nav.duplicates";
            case "Tags" -> "nav.tags";
            case "History" -> "nav.history";
            case "Settings" -> "nav.settings";

            case "All" -> "ui.all";
            case "Images" -> "ui.images";
            case "Videos" -> "ui.videos";
            case "Documents" -> "ui.documents";
            case "Audio" -> "ui.audio";
            case "Archives" -> "ui.archives";
            case "Code" -> "ui.code";
            case "Other" -> "ui.other";
            case "Name A-Z" -> "sort.name_asc";
            case "Name Z-A" -> "sort.name_desc";
            case "Size Small-Large" -> "sort.size_asc";
            case "Size Large-Small" -> "sort.size_desc";
            case "Newest" -> "sort.newest";
            case "Oldest" -> "sort.oldest";
            case "File Type" -> "ui.file_type";
            case "Strict" -> "ui.strict";
            case "Balanced" -> "ui.balanced";
            case "Broad" -> "ui.broad";
            case "Image" -> "type.image";
            case "Video" -> "ui.video_bc17c";
            case "Document" -> "ui.document_e214b";
            case "Archive" -> "ui.archive_2621c";
            case "File" -> "type.file";
            case "Folder" -> "type.folder";
            default -> null;
        };
    }
}
