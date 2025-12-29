package com.example.simplylearn.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class VideoService {

    private final PollyService pollyService;

    public VideoService(PollyService pollyService) {
        this.pollyService = pollyService;
    }
    public record VideoScene(
            String narration,
            String illustration
    ) {}


//    // ======================
//    // 🎬 MAIN ENTRY
//    // ======================
//    public void createVideo(
//            String videoScript,
//            Path imagesDir,
//            Path outputVideo
//    ) throws Exception {
//
//        List<VideoScene> scenes = parseScenes(videoScript);
//
//        List<Path> sceneVideos = new ArrayList<>();
//
//        int index = 0;
//        for (VideoScene scene : scenes) {
//
//            Path sceneAudio = imagesDir.resolve("scene-" + index + ".mp3");
//            Path sceneImage = imagesDir.resolve("scene-" + index + ".png");
//            Path sceneVideo = imagesDir.resolve("scene-" + index + ".mp4");
//
//            // 1️⃣ Generate audio (single speaker, generative)
//            pollyService.synthesizeSingleSpeakerPodcast(
//                    scene.narration(),
//                    sceneAudio
//            );
//
//            // 2️⃣ Create placeholder image (text-based for now)
//            ImageUtil.createPlaceholderImage(
//                    sceneImage,
//                    scene.illustration()
//            );
//
//            // 3️⃣ Create scene video with exact timing
//            runFFmpegScene(sceneImage, sceneAudio, sceneVideo);
//
//            sceneVideos.add(sceneVideo);
//            index++;
//        }
//
//        // 4️⃣ Concatenate all scenes
//        concatVideos(sceneVideos, outputVideo);
//    }
//
//    // ======================
//    // 🧠 PARSER
//    // ======================
//    private List<VideoScene> parseScenes(String script) {
//
//        List<VideoScene> scenes = new ArrayList<>();
//
//        String narration = null;
//        String illustration = null;
//
//        for (String line : script.split("\\r?\\n")) {
//            line = line.trim();
//
//            if (line.equals("SCENE:")) {
//                if (narration != null && illustration != null) {
//                    scenes.add(new VideoScene(narration, illustration));
//                }
//                narration = null;
//                illustration = null;
//            }
//            else if (line.startsWith("Narration:")) {
//                narration = line.substring(10).trim();
//            }
//            else if (line.startsWith("Illustration:")) {
//                illustration = line.substring(13).trim();
//            }
//        }
//
//        if (narration != null && illustration != null) {
//            scenes.add(new VideoScene(narration, illustration));
//        }
//
//        return scenes;
//    }
//
//    // ======================
//    // 🎥 FFmpeg HELPERS
//    // ======================
//    private void runFFmpegScene(Path image, Path audio, Path output) throws Exception {
//
//        ProcessBuilder pb = new ProcessBuilder(
//                "ffmpeg",
//                "-y",
//                "-loop", "1",
//                "-i", image.toString(),
//                "-i", audio.toString(),
//                "-c:v", "libx264",
//                "-c:a", "aac",
//                "-shortest",
//                "-pix_fmt", "yuv420p",
//                output.toString()
//        );
//
//        pb.redirectErrorStream(true);
//        Process p = pb.start();
//
//        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
//            while (r.readLine() != null) {}
//        }
//
//        p.waitFor();
//    }
//
//    private void concatVideos(List<Path> videos, Path output) throws Exception {
//
//        Path listFile = Files.createTempFile("ffmpeg-list", ".txt");
//
//        StringBuilder sb = new StringBuilder();
//        for (Path v : videos) {
//            sb.append("file '").append(v.toAbsolutePath()).append("'\n");
//        }
//
//        Files.writeString(listFile, sb.toString());
//
//        ProcessBuilder pb = new ProcessBuilder(
//                "ffmpeg",
//                "-y",
//                "-f", "concat",
//                "-safe", "0",
//                "-i", listFile.toString(),
//                "-c", "copy",
//                output.toString()
//        );
//
//        pb.inheritIO().start().waitFor();
//    }
}
