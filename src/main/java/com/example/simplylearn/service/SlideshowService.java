package com.example.simplylearn.service;

import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Service;

import java.awt.Rectangle;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlideshowService {

    private final ImageGenerationService imageGenerationService;

    public SlideshowService(ImageGenerationService imageGenerationService) {
        this.imageGenerationService = imageGenerationService;
    }

    public void createSlideshow(String aiText, Path outputPptx) throws Exception {
        List<SlideData> slides = parseSlides(aiText);

        try (XMLSlideShow ppt = new XMLSlideShow()) {
            for (SlideData slideData : slides) {
                createSlideWithImage(ppt, slideData);
            }

            try (OutputStream out = Files.newOutputStream(outputPptx)) {
                ppt.write(out);
            }
        }
    }

    private void createSlideWithImage(XMLSlideShow ppt, SlideData data) throws Exception {
        XSLFSlide slide = ppt.createSlide();

        // Title box at the top
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(50, 20, 620, 60));
        XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
        titleRun.setText(data.title);
        titleRun.setFontSize(28.0);
        titleRun.setBold(true);

        // Bullet points box on the left side
        XSLFTextBox bodyBox = slide.createTextBox();
        bodyBox.setAnchor(new Rectangle(50, 100, 620, 280));
        for (String bullet : data.bullets) {
            XSLFTextParagraph p = bodyBox.addNewTextParagraph();
            p.setBullet(true);
            XSLFTextRun r = p.addNewTextRun();
            r.setText(bullet);
            r.setFontSize(18.0);
        }

        // Generate and add image in the bottom right if illustration description exists
        if (data.illustration != null && !data.illustration.isBlank()) {
            try {
                System.out.println("Generating image for: " + data.illustration);
                byte[] imageBytes = imageGenerationService.generateSingleImage(data.illustration);

                // Add image to slide
                PictureData pictureData = ppt.addPicture(imageBytes, PictureData.PictureType.PNG);
                XSLFPictureShape picture = slide.createPicture(pictureData);

                // Position image in the BOTTOM RIGHT corner
                // Standard slide is 720 (width) x 540 (height) in PowerPoint units
                // Image: 280x280, positioned at bottom right with 20px margins
                picture.setAnchor(new Rectangle(420, 240, 280, 280));

                System.out.println("Image added to slide successfully");
            } catch (Exception e) {
                System.err.println("Failed to generate image for slide: " + e.getMessage());
                e.printStackTrace();

                // Fallback: add text description if image generation fails
                XSLFTextBox imageHint = slide.createTextBox();
                imageHint.setAnchor(new Rectangle(420, 460, 280, 60));
                XSLFTextRun hintRun = imageHint.addNewTextParagraph().addNewTextRun();
                hintRun.setText("Illustration idea: " + data.illustration);
                hintRun.setFontSize(12.0);
                hintRun.setItalic(true);
            }
        }
    }

    private List<SlideData> parseSlides(String text) {
        List<SlideData> slides = new ArrayList<>();
        SlideData current = null;

        for (String line : text.split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("Slide")) {
                if (current != null) {
                    slides.add(current);
                }
                current = new SlideData();
                current.title = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.startsWith("-") && current != null) {
                current.bullets.add(line.substring(1).trim());
            } else if (line.toLowerCase().startsWith("image:") && current != null) {
                current.illustration = line.substring(6).trim();
            }
        }

        if (current != null) {
            slides.add(current);
        }

        return slides;
    }

    // =====================================================
    // INTERNAL MODEL
    // =====================================================
    private static class SlideData {
        String title;
        List<String> bullets = new ArrayList<>();
        String illustration;
    }
}