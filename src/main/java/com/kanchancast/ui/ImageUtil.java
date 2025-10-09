package com.kanchancast.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Image helpers for product/gallery cards.
 * Provides a stable API expected by ProductCard & ProductDetailsDialog.
 */
public final class ImageUtil {

    // classpath fallback
    private static final String PLACEHOLDER_CLASSPATH = "/images/placeholder.png";

    /**
     * Returns an ImageView for a product image.
     * If {@code imagePath} is null/blank or fails to load, a placeholder is used.
     * Width/height are applied via fit sizes (preserveRatio=false).
     */
    public static ImageView getProductImage(String imagePath, double fitW, double fitH) {
        Image img = null;

        // Try an absolute/file path first (if provided)
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                // Supports absolute paths or relative "file:" URIs the user might save.
                String uri = imagePath.startsWith("file:") ? imagePath : "file:" + imagePath;
                img = new Image(uri, false);
            } catch (Exception ignore) {
                img = null;
            }
        }

        // Fallback to bundled placeholder
        if (img == null || img.isError()) {
            try {
                var in = ImageUtil.class.getResourceAsStream(PLACEHOLDER_CLASSPATH);
                if (in != null) {
                    img = new Image(in);
                }
            } catch (Exception ignore) {
                img = null;
            }
        }

        // Last‑ditch: draw a solid gray rect so UI never breaks
        if (img == null || img.isError()) {
            int w = (int) Math.max(1, fitW > 0 ? fitW : 300);
            int h = (int) Math.max(1, fitH > 0 ? fitH : 160);
            WritableImage wi = new WritableImage(w, h);
            PixelWriter pw = wi.getPixelWriter();
            Color c = Color.web("#D1D5DB"); // light gray
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    pw.setColor(x, y, c);
                }
            }
            img = wi;
        }

        ImageView iv = new ImageView(img);
        if (fitW > 0) iv.setFitWidth(fitW);
        if (fitH > 0) iv.setFitHeight(fitH);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        iv.setCache(true);
        return iv;
    }

    private ImageUtil() {}
}
