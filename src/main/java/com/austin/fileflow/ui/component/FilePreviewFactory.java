package com.austin.fileflow.ui.component;

import static com.austin.fileflow.i18n.I18n.tr;

import java.nio.file.Path;
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

    private final com.austin.fileflow.service.ThumbnailLoader thumbnails =
            new com.austin.fileflow.service.ThumbnailLoader();

    public Node createImagePreview(Path path, int width, int height, String fallback) {
        return new ThumbnailView(thumbnails, path, width, height, fallback);
    }

    public void close() { thumbnails.close(); }

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
        thumbnails.invalidate(path);

        nativeIconCache.remove(
                path.toAbsolutePath().normalize()
        );
    }

    public void clearCache() {
        thumbnails.clear();
    }

   private String getBadgeType(FileItem item) {

        if (item.isDirectory()) {
                return null;
        }

        return getBadgeTypeFromExtension(item.getExtension());
    }

    private String getBadgeTypeFromExtension(String extension) {

        if (extension == null) {
                return null;
        }

        String ext = extension
                .replaceFirst("^\\.", "")
                .toLowerCase(Locale.ROOT);

        return switch (ext) {

                case "pdf" -> "pdf";
                case "doc", "docx", "docm" -> "word";
                case "xls", "xlsx", "xlsm" -> "excel";
                case "ppt", "pptx", "pptm" -> "powerpoint";

                case "txt" -> "text";
                case "csv" -> "csv";
                case "json" -> "json";
                case "xml" -> "xml";

                case "java" -> "java";
                case "py", "pyw" -> "python";
                case "js", "jsx", "mjs", "cjs" -> "javascript";
                case "ts", "tsx", "mts", "cts" -> "typescript";

                case "md", "markdown" -> "markdown";
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

        Label placeholder = new Label(tr("files.app_badge"));

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
                    new Label(tr("type.folder"));

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
                return createSignatureTile(badgeType, preview);
        }

        if (hasNativeAppIcon(item)) {
                return createNativeAppPreview(item, preview);
        }

        if (item.getType() == FileType.IMAGE) {
            Node thumbnail = createImagePreview(item.getPath(), 56, 42, item.getExtension().toUpperCase(Locale.ROOT));
            thumbnail.getStyleClass().add("file-preview");
            return thumbnail;
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

    private Node createSignatureTile(
                String badgeType,
                StackPane preview
    ) {

        // 1. Add FileFlow badge styles
        preview.getStyleClass().addAll(
                "file-document-badge",
                "file-document-" + badgeType
        );

        // 2. Determine badge text
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

                default -> tr("ui.file");
        };

        // 3. Create text label
        Label badgeLabel = new Label(badgeText);

        badgeLabel.getStyleClass().add(
                "file-document-badge-text"
        );

        // 4. Create FileFlow Signature Flowline
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

        // 5. Assemble the complete tile
        preview.getChildren().addAll(
                flowline,
                badgeLabel
        );

        return preview;
    }

    public Node createStoredBadge(Path filePath) {

        if (filePath == null || filePath.getFileName() == null) {
                return null;
        }

        String fileName = filePath.getFileName().toString();

        int lastDot = fileName.lastIndexOf('.');

        // No valid extension
        if (lastDot <= 0 || lastDot == fileName.length() - 1) {
                return null;
        }

        String extension = fileName.substring(lastDot + 1);

        String badgeType = getBadgeTypeFromExtension(extension);

        // No signature badge available
        if (badgeType == null) {
                return null;
        }

        StackPane preview = new StackPane();

        preview.setMinSize(56, 42);
        preview.setPrefSize(56, 42);
        preview.setMaxSize(56, 42);

        preview.getStyleClass().add("file-preview");

        return createSignatureTile(badgeType, preview);
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

        Label badgeLabel = new Label(tr("ui.file"));

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
