package com.austin.fileflow.ui.component;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.austin.fileflow.model.FileItem;
import com.austin.fileflow.model.FileType;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class FilePreviewFactory {

    private final Map<Path, Image> imageThumbnailCache =
            new HashMap<>();

    public void invalidate(Path path) {
        imageThumbnailCache.remove(path);
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

        Label placeholder =
                new Label("FILE");

        placeholder.getStyleClass().add(
                "file-preview-placeholder"
        );

        preview.getChildren().add(
                placeholder
        );

        return preview;
    }
}