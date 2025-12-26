package com.example.simplylearn.service;

import com.example.simplylearn.model.FileUpload;
import com.example.simplylearn.repository.FileUploadRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class ConversionService {

    private final FileUploadRepository repo;
    private final StorageService storageService;
    private final OpenAIService openAIService;
    private final PollyService pollyService;
    private final SlideshowService slideshowService;
    private final VideoService videoService;

    public ConversionService(
            FileUploadRepository repo,
            StorageService storageService,
            OpenAIService openAIService,
            PollyService pollyService,
            SlideshowService slideshowService,
            VideoService videoService
    ) {
        this.repo = repo;
        this.storageService = storageService;
        this.openAIService = openAIService;
        this.pollyService = pollyService;
        this.slideshowService = slideshowService;
        this.videoService = videoService;
    }

    @Async
    @Transactional
    public void convert(UUID uploadId) {

        FileUpload upload = repo.findById(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("Upload not found"));

        try {
            upload.setStatus("PROCESSING");
            repo.save(upload);

            switch (upload.getConversionType().toLowerCase()) {
                case "podcast" -> handlePodcast(upload);
                case "summary" -> handleSummary(upload);
                case "slideshow" -> handleSlideshow(upload);
                case "video" -> handleVideo(upload);
                default -> throw new UnsupportedOperationException(
                        "Unsupported conversion type: " + upload.getConversionType()
                );
            }

            upload.setStatus("COMPLETED");
            repo.save(upload);

        } catch (Exception e) {
            e.printStackTrace();
            upload.setStatus("FAILED");
            repo.save(upload);
        }
    }

    // ======================
    // PODCAST
    // ======================
    private void handlePodcast(FileUpload upload) throws Exception {

        String text = readAllFiles(upload);

        String podcastScript = openAIService.createPodcastScript(text);

        Path mp3Path = storageService.resolveConverted(
                upload.getId().toString() + ".mp3"
        );

        pollyService.synthesizePodcastToMp3(podcastScript, mp3Path);

        upload.setConvertedFilename(mp3Path.getFileName().toString());
    }

    // ======================
    // SUMMARY
    // ======================
    private void handleSummary(FileUpload upload) throws Exception {

        String text = readAllFiles(upload);

        String summary = openAIService.createSummary(text);

        Path outPath = storageService.resolveConverted(
                upload.getId().toString() + ".txt"
        );

        Files.writeString(outPath, summary);

        upload.setConvertedFilename(outPath.getFileName().toString());
    }

    // ======================
    // SLIDESHOW
    // ======================
    private void handleSlideshow(FileUpload upload) throws Exception {

        String text = readAllFiles(upload);

        String slideshowOutline =
                openAIService.createSlideshowOutline(text);

        Path pptxPath = storageService.resolveConverted(
                upload.getId().toString() + ".pptx"
        );

        slideshowService.createSlideshow(slideshowOutline, pptxPath);

        upload.setConvertedFilename(pptxPath.getFileName().toString());
    }

    // ======================
    // 🎬 VIDEO
    // ======================
    private void handleVideo(FileUpload upload) throws Exception {

        String text = readAllFiles(upload);

        // 1️⃣ Create video narration script
        String videoScript =
                openAIService.createVideoScript(text);

        // 2️⃣ Create slideshow images (scene-based)
        Path imagesDir =
                storageService.createTempDirectory(upload.getId().toString());

        slideshowService.createVideoSlides(videoScript, imagesDir);

        // 3️⃣ Generate narration audio (ONE generative speaker)
        Path narrationMp3 =
                storageService.resolveConverted(upload.getId() + "-narration.mp3");

        pollyService.synthesizeVideoNarrationToMp3(videoScript, narrationMp3);

        // 4️⃣ Stitch into MP4 via FFmpeg
        Path videoPath =
                storageService.resolveConverted(upload.getId() + ".mp4");

        videoService.createVideo(videoScript, imagesDir, videoPath);

        upload.setConvertedFilename(videoPath.getFileName().toString());
    }

    // ======================
    // SHARED FILE READER
    // ======================
    private String readAllFiles(FileUpload upload) throws Exception {

        StringBuilder combined = new StringBuilder();

        String[] files = upload.getStoredFilename().split("\\|");

        for (String filename : files) {
            Path path = storageService.resolve(filename);

            if (!Files.exists(path)) {
                throw new IllegalStateException("Missing file: " + filename);
            }

            combined.append(Files.readString(path))
                    .append("\n\n");
        }

        return combined.toString().trim();
    }
}
