package com.gym.system.util;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public final class ImageFiles {
    private ImageFiles() {
    }

    public static WritableImage render(Parent document, double scale) {
        new Scene(document);
        document.applyCss();
        document.layout();
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setTransform(Transform.scale(scale, scale));
        parameters.setFill(Color.WHITE);
        return document.snapshot(parameters, null);
    }

    public static void savePng(Image image, File file) throws IOException {
        File target = file.getName().toLowerCase().endsWith(".png")
                ? file : new File(file.getParentFile(), file.getName() + ".png");
        if (!ImageIO.write(toBufferedImage(image), "png", target)) {
            throw new IOException("PNG encoding is not available.");
        }
    }

    public static BufferedImage toBufferedImage(Image image) {
        int width = (int) Math.round(image.getWidth());
        int height = (int) Math.round(image.getHeight());
        PixelReader reader = image.getPixelReader();
        if (reader == null) {
            throw new IllegalStateException("The image has no readable pixels.");
        }
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return result;
    }
}
