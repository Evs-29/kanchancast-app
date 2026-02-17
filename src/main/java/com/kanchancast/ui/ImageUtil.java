package com.kanchancast.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.File;
import java.nio.file.Path;

public final class ImageUtil {

    // classpath fallback
    private static final String PLACEHOLDER_CLASSPATH = "/images/placeholder.png";

    // folder where we will store selected product images
    public static Path appImagesDir() {
        String home = System.getProperty("user.home");
        return Path.of(home, "KanchanCast", "images", "products");
    }

    /**
     * Returns an ImageView for a product image.
     * Supports:
     *  - Absolute file paths
     *  - file: URIs
     *  - Relative file names saved into user.home/KanchanCast/images/products/
     */
    public static ImageView getProductImage(String imagePath, double fitW, double fitH) {
        Image img = null;

        if (imagePath != null && !imagePath.isBlank()) {
            try {
                String path = imagePath.trim();

                // If it's already a file: URI
                if (path.startsWith("file:")) {
                    img = new Image(path, false);
                } else {
                    File f = new File(path);

                    // Absolute/relative direct path
                    if (f.exists()) {
                        img = new Image(f.toURI().toString(), false);
                    } else {
                        // Try resolving relative name inside app images folder
                        File inside = appImagesDir().resolve(path).toFile();
                        if (inside.exists()) {
                            img = new Image(inside.toURI().toString(), false);
                        }
                    }
                }
            } catch (Exception ignore) {
                img = null;
            }
        }

        // Fallback to bundled placeholder
        if (img == null || img.isError()) {
            try {
                var in = ImageUtil.class.getResourceAsStream(PLACEHOLDER_CLASSPATH);
                if (in != null) img = new Image(in);
            } catch (Exception ignore) {
                img = null;
            }
        }

        // Last-ditch: draw a solid gray rect so UI never breaks
        if (img == null || img.isError()) {
            int w = (int) Math.max(1, fitW > 0 ? fitW : 300);
            int h = (int) Math.max(1, fitH > 0 ? fitH : 160);
            WritableImage wi = new WritableImage(w, h);
            PixelWriter pw = wi.getPixelWriter();
            Color c = Color.web("#D1D5DB");
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) pw.setColor(x, y, c);
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
