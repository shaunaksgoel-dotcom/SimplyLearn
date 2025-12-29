package com.example.simplylearn.service;

import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Service;

import java.awt.Rectangle;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlideshowService {

    public void createSlideshow(String aiText, Path outputPptx) throws Exception {
        List<SlideData> slides = parseSlides(aiText);
        XMLSlideShow ppt = new XMLSlideShow();
        try {
            OutputStream out = Files.newOutputStream(outputPptx, new java.nio.file.OpenOption[0]);
            try {
                for (SlideData slideData : slides)
                    createSlide(ppt, slideData);
                ppt.write(out);
                if (out != null)
                    out.close();
            } catch (Throwable throwable) {
                if (out != null)
                    try {
                        out.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                throw throwable;
            }
            ppt.close();
        } catch (Throwable throwable) {
            try {
                ppt.close();
            } catch (Throwable throwable1) {
                throwable.addSuppressed(throwable1);
            }
            throw throwable;
        }
    }

    private void createSlide(XMLSlideShow ppt, SlideData data) {
        XSLFSlide slide = ppt.createSlide();
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(50, 20, 620, 60));
        XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
        titleRun.setText(data.title);
        titleRun.setFontSize(Double.valueOf(28.0D));
        titleRun.setBold(true);
        XSLFTextBox bodyBox = slide.createTextBox();
        bodyBox.setAnchor(new Rectangle(50, 100, 620, 300));
        for (String bullet : data.bullets) {
            XSLFTextParagraph p = bodyBox.addNewTextParagraph();
            p.setBullet(true);
            XSLFTextRun r = p.addNewTextRun();
            r.setText(bullet);
            r.setFontSize(Double.valueOf(18.0D));
        }
        if (data.illustration != null && !data.illustration.isBlank()) {
            XSLFTextBox imageHint = slide.createTextBox();
            imageHint.setAnchor(new Rectangle(50, 420, 620, 40));
            XSLFTextRun hintRun = imageHint.addNewTextParagraph().addNewTextRun();
            hintRun.setText("Illustration idea: " + data.illustration);
            hintRun.setFontSize(Double.valueOf(12.0D));
            hintRun.setItalic(true);
        }
    }

    private List<SlideData> parseSlides(String text) {
        List<SlideData> slides = new ArrayList<>();
        SlideData current = null;
        for (String line : text.split("\\r?\\n")) {
            line = line.trim();
            if (!line.isEmpty())
                if (line.startsWith("Slide")) {
                    if (current != null)
                        slides.add(current);
                    current = new SlideData();
                    current.title = line.substring(line.indexOf(":") + 1).trim();
                } else if (line.startsWith("-") && current != null) {
                    current.bullets.add(line.substring(1).trim());
                } else if (!line.startsWith("-") && current != null) {
                    current.illustration = line;
                }
        }
        if (current != null)
            slides.add(current);
        return slides;
    }
//    // =====================================================
//    // 🎬 VIDEO SCENES (TEMP SLIDES)
//    // =====================================================
//    public void createVideoSlides(String videoScript, Path outputDir)
//            throws Exception {
//
//        Files.createDirectories(outputDir);
//
//        List<String> scenes = parseScenes(videoScript);
//
//        try (XMLSlideShow ppt = new XMLSlideShow()) {
//
//            for (int i = 0; i < scenes.size(); i++) {
//
//                XSLFSlide slide = ppt.createSlide();
//
//                XSLFTextBox textBox = slide.createTextBox();
//                textBox.setAnchor(new java.awt.Rectangle(50, 100, 620, 300));
//
//                XSLFTextRun run =
//                        textBox.addNewTextParagraph().addNewTextRun();
//                run.setText(scenes.get(i));
//                run.setFontSize(20.0);
//
//                Path slideFile =
//                        outputDir.resolve("scene-" + i + ".pptx");
//
//                try (OutputStream out =
//                             Files.newOutputStream(slideFile)) {
//                    ppt.write(out);
//                }
//
//                ppt.getSlides().clear();
//            }
//        }
//    }
//
//    private List<String> parseScenes(String script) {
//
//        List<String> scenes = new ArrayList<>();
//        StringBuilder current = new StringBuilder();
//
//        for (String line : script.split("\\r?\\n")) {
//
//            line = line.trim();
//            if (line.isEmpty()) continue;
//
//            if (line.equals("[[SCENE_BREAK]]")) {
//                scenes.add(current.toString().trim());
//                current.setLength(0);
//            } else {
//                current.append(line).append(" ");
//            }
//        }
//
//        if (!current.isEmpty()) {
//            scenes.add(current.toString().trim());
//        }
//
//        return scenes;
//    }

    // =====================================================
    // INTERNAL MODEL
    // =====================================================
    private static class SlideData {
        String title;
        List<String> bullets = new ArrayList<>();
        String illustration;
    }
}
