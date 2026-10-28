package com.austin.fileflow.ui.component;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

import com.austin.fileflow.model.FileItem;
import com.austin.fileflow.model.FileType;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;

import javafx.application.Platform;

public class FilePreviewFactory {

    private final Map<Path, Image> imageThumbnailCache =
            new HashMap<>();

    private final Map<Path, CompletableFuture<byte[]>> nativeIconCache =
        new ConcurrentHashMap<>();

    private static final ExecutorService NATIVE_ICON_EXECUTOR =
        Executors.newFixedThreadPool(2, runnable -> {

            Thread thread = new Thread(
                    runnable,
                    "fileflow-native-icon"
            );

            thread.setDaemon(true);

            return thread;
        });

    public void invalidate(Path path) {
        imageThumbnailCache.remove(path);

        nativeIconCache.remove(
                path.toAbsolutePath().normalize()
        );
    }

    public void clearCache() {
        imageThumbnailCache.clear();
    }

    private String getBadgeType(FileItem item) {

        if (item.isDirectory() || item.getExtension() == null) {
                return null;
        }

        String extension = item.getExtension()
                .replaceFirst("^\\.", "")
                .toLowerCase(Locale.ROOT);

        return switch (extension) {

                // Office & PDF
                case "pdf" -> "pdf";
                case "doc", "docx", "docm" -> "word";
                case "xls", "xlsx", "xlsm" -> "excel";
                case "ppt", "pptx", "pptm" -> "powerpoint";

                // Text & Data
                case "txt" -> "text";
                case "csv" -> "csv";
                case "json" -> "json";
                case "xml" -> "xml";

                // Programming Languages
                case "java" -> "java";
                case "py", "pyw" -> "python";
                // Web Development
                case "js", "jsx", "mjs", "cjs" -> "javascript";
                case "ts", "tsx", "mts", "cts" -> "typescript";
                case "md", "markdown" -> "markdown";

                // Web & Data Files
                case "html", "htm" -> "html";
                case "css" -> "css";
                case "yaml", "yml" -> "yaml";
                case "sql" -> "sql";

                default -> null;
        };
    }

    public boolean hasSignatureBadge(FileItem item) {
        return getBadgeType(item) != null;
    }

    public boolean hasNativeAppIcon(FileItem item) {

        if (item.isDirectory() || item.getExtension() == null) {
                return false;
        }

        // Native Windows application icons
        String os = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT);

        if (!os.contains("win")) {
                return false;
        }

        String extension = item.getExtension()
                .replaceFirst("^\\.", "")
                .toLowerCase(Locale.ROOT);

        return switch (extension) {
                case "exe", "lnk", "url", "msi" -> true;
                default -> false;
        };
    }

    private byte[] readNativeIcon(Path path) {

        try {

                if (!Files.isRegularFile(path)) {
                        return null;
                }

                Icon icon = FileSystemView
                        .getFileSystemView()
                        .getSystemIcon(path.toFile(), 32, 32);

                if (icon == null
                        || icon.getIconWidth() <= 0
                        || icon.getIconHeight() <= 0) {
                        return null;
                }

                BufferedImage bufferedImage = new BufferedImage(
                        icon.getIconWidth(),
                        icon.getIconHeight(),
                        BufferedImage.TYPE_INT_ARGB
                );

                Graphics2D graphics = bufferedImage.createGraphics();

                try {
                        icon.paintIcon(null, graphics, 0, 0);
                } finally {
                        graphics.dispose();
                }

                ByteArrayOutputStream output =
                        new ByteArrayOutputStream();

                if (!ImageIO.write(bufferedImage, "png", output)) {
                        return null;
                }

                return output.toByteArray();

        } catch (IOException | RuntimeException exception) {
                return null;
        }
    }

    private Node createNativeAppPreview(
                FileItem item,
                StackPane preview
    ) {

        Path path = item.getPath()
                .toAbsolutePath()
                .normalize();

        Label placeholder = new Label("APP");

        placeholder.getStyleClass().add(
                "file-native-app-placeholder"
        );

        preview.getStyleClass().add(
                "file-native-app-preview"
        );

        preview.getChildren().add(placeholder);

        CompletableFuture<byte[]> future =
                nativeIconCache.computeIfAbsent(
                        path,
                        key -> CompletableFuture.supplyAsync(
                                () -> readNativeIcon(key),
                                NATIVE_ICON_EXECUTOR
                        )
                );

        future.thenAccept(bytes -> {

                if (bytes == null || bytes.length == 0) {
                        return;
                }

                Platform.runLater(() -> {

                        // Ignore an invalidated or replaced cache entry.
                        if (nativeIconCache.get(path) != future) {
                                return;
                        }

                        Image image = new Image(
                                new ByteArrayInputStream(bytes)
                        );

                        if (image.isError()) {
                                return;
                        }

                        ImageView iconView = new ImageView(image);

                        iconView.setFitWidth(30);
                        iconView.setFitHeight(30);
                        iconView.setPreserveRatio(true);
                        iconView.setSmooth(true);

                        preview.getChildren().setAll(iconView);

                        preview.getStyleClass().add(
                                "file-native-app-loaded"
                        );
                });
        });

        return preview;
    }

    public Node create(FileItem item) {

        StackPane preview =
                new StackPane();

        preview.setPrefSize(
                56,
                42
        );

        preview.setMinSize(
                56,
                42
        );

        preview.setMaxSize(
                56,
                42
        );

        preview.getStyleClass().add(
                "file-preview"
        );

        if (item.isDirectory()) {

            Label folderLabel =
                    new Label("Folder");

            folderLabel.getStyleClass().add(
                    "file-preview-placeholder"
            );

            preview.getChildren().add(
                    folderLabel
            );

            return preview;
        }

        String badgeType = getBadgeType(item);

        if (badgeType != null) {

                String badgeText = switch (badgeType) {

                        case "pdf" -> "PDF";
                        case "word" -> "W";
                        case "excel" -> "X";
                        case "powerpoint" -> "P";

                        case "text" -> "TXT";
                        case "csv" -> "CSV";
                        case "json" -> "JSON";
                        case "xml" -> "XML";
                        case "java" -> "JAVA";
                        case "python" -> "PY";
                        case "javascript" -> "JS";
                        case "typescript" -> "TS";
                        case "markdown" -> "MD";

                        case "html" -> "HTML";
                        case "css" -> "CSS";
                        case "yaml" -> "YAML";
                        case "sql" -> "SQL";

                        default -> "FILE";
                };

                Label badgeLabel = new Label(badgeText);

                badgeLabel.getStyleClass().add(
                        "file-document-badge-text"
                );

                preview.getStyleClass().addAll(
                        "file-document-badge",
                        "file-document-" + badgeType
                );

                // FileFlow Signature Flowline
                Region flowline = new Region();

                flowline.getStyleClass().add(
                        "fileflow-badge-flowline"
                );

                flowline.setMinSize(3, 18);
                flowline.setPrefSize(3, 18);
                flowline.setMaxSize(3, 18);

                // Position the signature mark on the left
                StackPane.setAlignment(flowline, Pos.CENTER_LEFT);

                StackPane.setMargin(
                        flowline,
                        new Insets(0, 0, 0, 6)
                );

                // Keep the document letter centred
                preview.getChildren().addAll(
                        flowline,
                        badgeLabel
                );

                return preview;
        }

        if (hasNativeAppIcon(item)) {
                return createNativeAppPreview(item, preview);
        }

        if (item.getType() == FileType.IMAGE) {

            Image image =
                    imageThumbnailCache.computeIfAbsent(
                            item.getPath(),
                            path -> new Image(
                                    path.toUri().toString(),
                                    112,
                                    84,
                                    true,
                                    true,
                                    true
                            )
                    );

            if (image.isError()) {

                Label formatLabel =
                        new Label(
                                item.getExtension()
                                        .toUpperCase()
                        );

                formatLabel.getStyleClass().add(
                        "file-preview-placeholder"
                );

                preview.getChildren().add(
                        formatLabel
                );

                return preview;
            }

            ImageView imageView =
                    new ImageView(image);

            image.errorProperty().addListener(
                    (observable, oldValue, hasError) -> {

                        if (!hasError) {
                            return;
                        }

                        preview.getChildren().clear();

                        Label formatLabel =
                                new Label(
                                        item.getExtension()
                                                .toUpperCase()
                                );

                        formatLabel.getStyleClass().add(
                                "file-preview-placeholder"
                        );

                        preview.getChildren().add(
                                formatLabel
                        );
                    }
            );

            imageView.setFitWidth(56);
            imageView.setFitHeight(42);
            imageView.setPreserveRatio(true);

            preview.getChildren().add(
                    imageView
            );

            preview.getStyleClass().add(
                    "file-preview-image"
            );

            return preview;
        }

        if (item.getType() == FileType.VIDEO) {

            Label playLabel =
                    new Label("▶");

            playLabel.getStyleClass().add(
                    "file-preview-play"
            );

            preview.getChildren().add(
                    playLabel
            );

            preview.getStyleClass().add(
                    "file-preview-video"
            );

            return preview;
        }

        Label placeholder =
                new Label(
                        item.getExtension()
                                .toUpperCase()
                );

        placeholder.getStyleClass().add(
                "file-preview-placeholder"
        );

        preview.getChildren().add(
                placeholder
        );

        return preview;
    }

    public Node createFallback() {

        StackPane preview = new StackPane();

        preview.setMinSize(56, 42);
        preview.setPrefSize(56, 42);
        preview.setMaxSize(56, 42);

        preview.getStyleClass().addAll(
                "file-preview",
                "file-document-badge",
                "file-document-generic"
        );

        Label badgeLabel = new Label("FILE");

        badgeLabel.getStyleClass().add(
                "file-document-badge-text"
        );

        Region flowline = new Region();

        flowline.getStyleClass().add(
                "fileflow-badge-flowline"
        );

        flowline.setMinSize(3, 18);
        flowline.setPrefSize(3, 18);
        flowline.setMaxSize(3, 18);

        StackPane.setAlignment(
                flowline,
                Pos.CENTER_LEFT
        );

        StackPane.setMargin(
                flowline,
                new Insets(0, 0, 0, 6)
        );

        preview.getChildren().addAll(
                flowline,
                badgeLabel
        );

        return preview;
    }
}