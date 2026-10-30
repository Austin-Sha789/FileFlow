package com.austin.fileflow.ui;

import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OceanWindowChromeTest {
    private final Rectangle2D original = new Rectangle2D(100, 80, 1280, 800);

    @Test void draggingBottomRightResizesWithoutMovingTheWindow() {
        Rectangle2D resized = OceanWindowChrome.resizeBounds(original, 140, 90, false, true, false, true, 1060, 680);
        assertEquals(new Rectangle2D(100, 80, 1420, 890), resized);
    }

    @Test void draggingTopLeftKeepsTheOppositeCornerAnchored() {
        Rectangle2D resized = OceanWindowChrome.resizeBounds(original, -50, -30, true, false, true, false, 1060, 680);
        assertEquals(original.getMaxX(), resized.getMaxX());
        assertEquals(original.getMaxY(), resized.getMaxY());
        assertEquals(new Rectangle2D(50, 50, 1330, 830), resized);
    }

    @Test void minimumSizeKeepsTopAndLeftResizeAnchorsConsistent() {
        Rectangle2D resized = OceanWindowChrome.resizeBounds(original, 1000, 900, true, false, true, false, 1060, 680);
        assertEquals(1060, resized.getWidth());
        assertEquals(680, resized.getHeight());
        assertEquals(original.getMaxX(), resized.getMaxX());
        assertEquals(original.getMaxY(), resized.getMaxY());
    }

    @Test void horizontalResizeDoesNotChangeHeightOrVerticalPosition() {
        Rectangle2D resized = OceanWindowChrome.resizeBounds(original, -900, 400, false, true, false, false, 1060, 680);
        assertEquals(new Rectangle2D(100, 80, 1060, 800), resized);
    }
}
