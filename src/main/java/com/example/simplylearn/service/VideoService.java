package com.example.simplylearn.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class VideoService {

    private final PollyService pollyService;

    public VideoService(PollyService pollyService) {
        this.pollyService = pollyService;
    }

    // ======================
    // 🎬 INTERNAL MODEL
    // ======================
    public record VideoScene(
            String narration,
            String illustration
    ) {}

    // ======================
    // 🎬 MAIN ENTRY
    // ======================

    public void createVideo(
            String videoScript,
            Path imagesDir,
            Path outputVideo
    ) throws Exception {
        ProcessBuilder check = new ProcessBuilder("ffmpeg", "-version");
        try {
            Process p = check.start();
            if (p.waitFor() != 0) {
                throw new IllegalStateException("FFmpeg is not available on the system");
            }
        } catch (Exception e) {
            throw new IllegalStateException("FFmpeg is not installed or not in PATH", e);
        }

        Files.createDirectories(imagesDir);

        List<VideoScene> scenes = parseScenes(videoScript);
        if (scenes.isEmpty()) {
            throw new IllegalStateException("No video scenes were parsed");
        }

        List<Path> sceneVideos = new ArrayList<>();

        int index = 0;
        for (VideoScene scene : scenes) {

            Path sceneAudio = imagesDir.resolve("scene-" + index + ".mp3");
            Path sceneImage = imagesDir.resolve("scene-" + index + ".png");
            Path sceneVideo = imagesDir.resolve("scene-" + index + ".mp4");

            // 1️⃣ Generate narration audio (single speaker, generative)
            pollyService.synthesizeSingleSpeakerPodcast(
                    scene.narration(),
                    sceneAudio
            );

            // 2️⃣ Generate placeholder image (text-based for now)
            ImageUtil.createPlaceholderImage(
                    sceneImage,
                    scene.illustration()
            );

            // 3️⃣ Create video scene
            runFFmpegScene(sceneImage, sceneAudio, sceneVideo);

            sceneVideos.add(sceneVideo);
            index++;
        }

        // 4️⃣ Concatenate all scenes into final MP4
        concatVideos(sceneVideos, outputVideo);
    }

    // ======================
    // 🧠 SCRIPT PARSER
    // ======================
    private List<VideoScene> parseScenes(String script) {

        List<VideoScene> scenes = new ArrayList<>();

        String narration = null;
        String illustration = null;

        for (String rawLine : script.split("\\r?\\n")) {

            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            if (line.equals("SCENE:")) {
                if (narration != null && illustration != null) {
                    scenes.add(new VideoScene(narration, illustration));
                }
                narration = null;
                illustration = null;
            }
            else if (line.startsWith("Narration:")) {
                narration = line.substring("Narration:".length()).trim();
            }
            else if (line.startsWith("Illustration:")) {
                illustration = line.substring("Illustration:".length()).trim();
            }
        }

        if (narration != null && illustration != null) {
            scenes.add(new VideoScene(narration, illustration));
        }

        return scenes;
    }

    // ======================
    // 🎥 FFmpeg: SINGLE SCENE
    // ======================
    private void runFFmpegScene(Path image, Path audio, Path output) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-loop", "1",
                "-i", image.toAbsolutePath().toString(),
                "-i", audio.toAbsolutePath().toString(),
                "-c:v", "libx264",
                "-tune", "stillimage",
                "-c:a", "aac",
                "-shortest",
                "-pix_fmt", "yuv420p",
                output.toAbsolutePath().toString()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {}
        }

        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("FFmpeg scene render failed");
        }
    }

    // ======================
    // 🎞️ FFmpeg CONCAT (CRITICAL FIX)
    // ======================
    private void concatVideos(List<Path> videos, Path output) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-protocol_whitelist", "file,pipe,fd",
                "-i", "pipe:0",
                "-c:v", "libx264",
                "-c:a", "aac",
                "-pix_fmt", "yuv420p",
                "-movflags", "+faststart",
                output.toAbsolutePath().toString()
        );

        Process process = pb.start();

        // Write concat list to stdin
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {

            for (Path v : videos) {
                writer.write("file '" + v.toAbsolutePath() + "'");
                writer.newLine();
            }
            writer.flush();
        }

        String errorOutput;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            errorOutput = reader.lines().reduce("", (a, b) -> a + "\n" + b);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg concat failed:\n" + errorOutput);
        }
    }
}
