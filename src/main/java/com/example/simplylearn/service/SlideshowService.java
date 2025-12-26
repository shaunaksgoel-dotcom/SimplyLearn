package com.example.simplylearn.service;

import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlideshowService {

    // =====================================================
    // 🎞 SLIDESHOW (PPTX)
    // =====================================================
    public void createSlideshow(String aiText, Path outputPptx) throws Exception {

        List<SlideData> slides = parseSlides(aiText);

        try (XMLSlideShow ppt = new XMLSlideShow();
             OutputStream out = Files.newOutputStream(outputPptx)) {

            for (SlideData slideData : slides) {
                createSlide(ppt, slideData);
            }

            ppt.write(out);
        }
    }

    private void createSlide(XMLSlideShow ppt, SlideData data) {

        XSLFSlide slide = ppt.createSlide();

        // ===== TITLE =====
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new java.awt.Rectangle(50, 20, 620, 60));

        XSLFTextRun titleRun =
                titleBox.addNewTextParagraph().addNewTextRun();
        titleRun.setText(data.title);
        titleRun.setFontSize(28.0);
        titleRun.setBold(true);

        // ===== BULLETS =====
        XSLFTextBox bodyBox = slide.createTextBox();
        bodyBox.setAnchor(new java.awt.Rectangle(50, 100, 620, 300));

        for (String bullet : data.bullets) {
            XSLFTextParagraph p = bodyBox.addNewTextParagraph();
            p.setBullet(true);

            XSLFTextRun r = p.addNewTextRun();
            r.setText(bullet);
            r.setFontSize(18.0);
        }

        // ===== ILLUSTRATION NOTE =====
        if (data.illustration != null && !data.illustration.isBlank()) {
            XSLFTextBox hint = slide.createTextBox();
            hint.setAnchor(new java.awt.Rectangle(50, 420, 620, 40));

            XSLFTextRun r =
                    hint.addNewTextParagraph().addNewTextRun();
            r.setText("Illustration idea: " + data.illustration);
            r.setFontSize(12.0);
            r.setItalic(true);
        }
    }

    private List<SlideData> parseSlides(String text) {

        List<SlideData> slides = new ArrayList<>();
        SlideData current = null;

        for (String line : text.split("\\r?\\n")) {

            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("Slide")) {
                if (current != null) slides.add(current);
                current = new SlideData();
                current.title =
                        line.substring(line.indexOf(":") + 1).trim();
            }
            else if (line.startsWith("-") && current != null) {
                current.bullets.add(line.substring(1).trim());
            }
            else if (current != null) {
                current.illustration = line;
            }
        }

        if (current != null) slides.add(current);
        return slides;
    }

    // =====================================================
    // 🎬 VIDEO SCENES (TEMP SLIDES)
    // =====================================================
    public void createVideoSlides(String videoScript, Path outputDir)
            throws Exception {

        Files.createDirectories(outputDir);

        List<String> scenes = parseScenes(videoScript);

        try (XMLSlideShow ppt = new XMLSlideShow()) {

            for (int i = 0; i < scenes.size(); i++) {

                XSLFSlide slide = ppt.createSlide();

                XSLFTextBox textBox = slide.createTextBox();
                textBox.setAnchor(new java.awt.Rectangle(50, 100, 620, 300));

                XSLFTextRun run =
                        textBox.addNewTextParagraph().addNewTextRun();
                run.setText(scenes.get(i));
                run.setFontSize(20.0);

                Path slideFile =
                        outputDir.resolve("scene-" + i + ".pptx");

                try (OutputStream out =
                             Files.newOutputStream(slideFile)) {
                    ppt.write(out);
                }

                ppt.getSlides().clear();
            }
        }
    }

    private List<String> parseScenes(String script) {

        List<String> scenes = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : script.split("\\r?\\n")) {

            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.equals("[[SCENE_BREAK]]")) {
                scenes.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(line).append(" ");
            }
        }

        if (!current.isEmpty()) {
            scenes.add(current.toString().trim());
        }

        return scenes;
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
