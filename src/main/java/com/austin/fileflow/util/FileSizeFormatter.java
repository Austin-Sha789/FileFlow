package com.austin.fileflow.util;

public final class FileSizeFormatter {

    private FileSizeFormatter() {
    }

    public static String format(long bytes) {
        double kilobytes = bytes / 1024.0;
        double megabytes = kilobytes / 1024.0;
        double gigabytes = megabytes / 1024.0;
        double terabytes = gigabytes / 1024.0;

        if (terabytes >= 1) {
            return String.format("%.1f TB", terabytes);
        }

        if (gigabytes >= 1) {
            return String.format("%.1f GB", gigabytes);
        }

        if (megabytes >= 1) {
            return String.format("%.1f MB", megabytes);
        }

        if (kilobytes >= 1) {
            return String.format("%.1f KB", kilobytes);
        }

        return bytes + " B";
    }
}