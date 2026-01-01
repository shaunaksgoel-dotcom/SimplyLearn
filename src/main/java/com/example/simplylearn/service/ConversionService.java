package com.example.simplylearn.service;

import com.example.simplylearn.model.FileUpload;
import com.example.simplylearn.repository.FileUploadRepository;
import com.example.simplylearn.service.OpenAIService;
import com.example.simplylearn.service.PollyService;
import com.example.simplylearn.service.SlideshowService;
import com.example.simplylearn.service.StorageService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversionService {
    private final FileUploadRepository repo;

    private final StorageService storageService;

    private final OpenAIService openAIService;

    private final PollyService pollyService;

    private final SlideshowService slideshowService;

    private final AudioService audioService;

    private final ImageGenerationService imageService;

    private final VideoService videoService;

    public ConversionService(FileUploadRepository repo, StorageService storageService, OpenAIService openAIService, PollyService pollyService, SlideshowService slideshowService, AudioService audioService, ImageGenerationService imageService, VideoService videoService) {
        this.repo = repo;
        this.storageService = storageService;
        this.openAIService = openAIService;
        this.pollyService = pollyService;
        this.slideshowService = slideshowService;
        this.audioService = audioService;
        this.imageService = imageService;
        this.videoService = videoService;
    }

    @Async
    @Transactional
    public void convert(UUID uploadId) {
        FileUpload upload = (FileUpload)this.repo.findById(uploadId).orElseThrow(() -> new IllegalArgumentException("Upload not found"));
        try {
            upload.setStatus("PROCESSING");
            this.repo.save(upload);
            switch (upload.getConversionType().toLowerCase()) {
                case "podcast":
                    handlePodcast(upload);
                    break;
                case "summary":
                    handleSummary(upload);
                    break;
                case "slideshow":
                    handleSlideshow(upload);
                    break;
                case "quiz":
                    handleQuiz(upload);
                    break;
                case "video":
                    handleVideo(upload);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported conversion type: " + upload
                            .getConversionType());
            }
            upload.setStatus("COMPLETED");
            this.repo.save(upload);
        } catch (Exception e) {
            e.printStackTrace();
            upload.setStatus("FAILED");
            this.repo.save(upload);
        }
    }

    private void handlePodcast(FileUpload upload) throws Exception {
        String text = readAllFiles(upload);
        String podcastScript = this.openAIService.createPodcastScript(text);
        Path mp3Path = this.storageService.resolveConverted(upload
                .getId().toString() + ".mp3");
        this.pollyService.synthesizePodcastToMp3(podcastScript, mp3Path);
        upload.setConvertedFilename(mp3Path.getFileName().toString());
    }

    private void handleSummary(FileUpload upload) throws Exception {
        String text = readAllFiles(upload);
        String summary = this.openAIService.createSummary(text);
        Path outPath = this.storageService.resolveConverted(upload
                .getId().toString() + ".txt");
        Files.writeString(outPath, summary, new java.nio.file.OpenOption[0]);
        upload.setConvertedFilename(outPath.getFileName().toString());
    }

    private void handleSlideshow(FileUpload upload) throws Exception {
        String text = readAllFiles(upload);
        String slideshowOutline = this.openAIService.createSlideshowOutline(text);
        Path pptxPath = this.storageService.resolveConverted(upload
                .getId().toString() + ".pptx");
        this.slideshowService.createSlideshow(slideshowOutline, pptxPath);
        upload.setConvertedFilename(pptxPath.getFileName().toString());
    }
    private void handleVideo(FileUpload upload) throws Exception {
        String text = readAllFiles(upload);
        String videoScript = this.openAIService.createVideoOutline(text);
        String uploadId = upload.getId().toString();

        // Generate and upload audio files to S3
        this.audioService.generateAudioFromScenes(videoScript, uploadId);

        // Generate and upload image files to S3
        this.imageService.generateImagesFromScenes(videoScript, uploadId);

        // Stitch into MP4
        Path videoPath = this.storageService.resolveConverted(uploadId + ".mp4");
        this.videoService.createVideoFromScenes(uploadId, videoPath);

        upload.setConvertedFilename(videoPath.getFileName().toString());
    }
    private void handleQuiz(FileUpload upload) throws Exception{
        String text = readAllFiles(upload);
        String quizContent = this.openAIService.createQuiz(text);

        // This code is writing the json into a file and saving the filename in the db
        Path outPath = this.storageService.resolveConverted(upload
                .getId().toString() + ".txt");
        Files.writeString(outPath, quizContent, new java.nio.file.OpenOption[0]);
        upload.setConvertedFilename(outPath.getFileName().toString());
    }

    private String readAllFiles(FileUpload upload) throws Exception {
        StringBuilder combined = new StringBuilder();
        String[] files = upload.getStoredFilename().split("\\|");
        for (String filename : files) {
            Path path = this.storageService.resolve(filename);
            if (!Files.exists(path, new java.nio.file.LinkOption[0]))
                throw new IllegalStateException("Missing file: " + filename);
            combined.append(Files.readString(path))
                    .append("\n\n");
        }
        return combined.toString().trim();
    }
}
