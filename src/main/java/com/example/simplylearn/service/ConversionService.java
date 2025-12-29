package com.example.simplylearn.service;

import com.example.simplylearn.model.ConversionType;
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


            switch (upload.getConversionType()) {
                case PODCAST -> handlePodcast(upload);
                case SUMMARY -> handleSummary(upload);
                case SLIDESHOW -> handleSlideshow(upload);
                case VIDEO -> handleVideo(upload);
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
    // 🎙 PODCAST
    // ======================
    private void handlePodcast(FileUpload upload) throws Exception {

        String text = readAllFiles(upload);
        String script = openAIService.createPodcastScript(text);

        Path mp3 = storageService.resolveConverted(upload.getId() + ".mp3");
        pollyService.synthesizePodcastToMp3(script, mp3);

        upload.setConvertedFilename(mp3.getFileName().toString());
    }

    // ======================
    // 📝 SUMMARY
    // ======================
    private void handleSummary(FileUpload upload) throws Exception {

        String text = readAllFiles(upload);
        String summary = openAIService.createSummary(text);

        Path out = storageService.resolveConverted(upload.getId() + ".txt");
        Files.writeString(out, summary);

        upload.setConvertedFilename(out.getFileName().toString());
    }

    // ======================
    // 📊 SLIDESHOW
    // ======================
    private void handleSlideshow(FileUpload upload) throws Exception {

        String text = readAllFiles(upload);
        String outline = openAIService.createSlideshowOutline(text);

        Path pptx = storageService.resolveConverted(upload.getId() + ".pptx");
        slideshowService.createSlideshow(outline, pptx);

        upload.setConvertedFilename(pptx.getFileName().toString());
    }

    // ======================
    // 🎬 VIDEO (FIXED)
    // ======================
    private void handleVideo(FileUpload upload) throws Exception {

        String text = readAllFiles(upload);

        // 1️⃣ Generate structured video script
        String script = openAIService.createVideoScript(text);

        // 2️⃣ Create TEMP directory for scene assets
        Path imagesDir = Files.createTempDirectory(
                "video-" + upload.getId()
        );

        // 3️⃣ Generate placeholder slides (1 per scene)
        slideshowService.createVideoSlides(script, imagesDir);

        // 4️⃣ Render MP4 (audio + ffmpeg inside VideoService)
        Path video = storageService.resolveConverted(upload.getId() + ".mp4");

        videoService.createVideo(script, imagesDir, video);

        upload.setConvertedFilename(video.getFileName().toString());
    }

    // ======================
    // 📂 FILE READER
    // ======================
    private String readAllFiles(FileUpload upload) throws Exception {

        StringBuilder combined = new StringBuilder();

        for (String f : upload.getStoredFilename().split("\\|")) {
            Path p = storageService.resolve(f);
            combined.append(Files.readString(p)).append("\n\n");
        }

        return combined.toString().trim();
    }

}
