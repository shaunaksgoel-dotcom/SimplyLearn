package com.example.simplylearn.service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

public class ImageUtil {

    public static void createPlaceholderImage(Path path, String text) throws Exception {

        BufferedImage img = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1280, 720);

        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 32));

        drawCenteredText(g, text, 1280, 720);

        g.dispose();
        ImageIO.write(img, "png", path.toFile());
    }

    private static void drawCenteredText(Graphics2D g, String text, int w, int h) {
        FontMetrics fm = g.getFontMetrics();
        int x = Math.max(40, (w - fm.stringWidth(text)) / 2);
        int y = h / 2;
        g.drawString(text, x, y);
    }
}
