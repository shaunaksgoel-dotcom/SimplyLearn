package com.example.simplylearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class VideoService {

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final String bucketName;

    public VideoService(@Value("${aws.s3.bucket.name:simplylearn-video-scenes}") String bucketName) {
        this.bucketName = bucketName;
        this.objectMapper = new ObjectMapper();

        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    @PreDestroy
    public void close() {
        s3Client.close();
    }

    /**
     * Download files from S3, stitch them into an MP4, and save to output path
     * @param uploadId The upload ID (S3 folder name)
     * @param outputPath Where to save the final MP4
     */
    public void createVideoFromScenes(String uploadId, Path outputPath) throws Exception {
        // Create temporary directory for downloaded files
        Path tempDir = Files.createTempDirectory("video-scenes-" + uploadId);

        try {
            System.out.println("Downloading scene files from S3...");

            // List all files in the S3 folder
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(uploadId + "/")
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            // Download all files
            List<String> audioFiles = new ArrayList<>();
            List<String> imageFiles = new ArrayList<>();

            for (S3Object s3Object : listResponse.contents()) {
                String key = s3Object.key();
                String fileName = key.substring(key.lastIndexOf('/') + 1);

                if (fileName.isEmpty()) continue; // Skip the folder itself

                Path localPath = tempDir.resolve(fileName);
                downloadFile(key, localPath);

                if (fileName.startsWith("audioScene")) {
                    audioFiles.add(localPath.toString());
                } else if (fileName.startsWith("imageScene")) {
                    imageFiles.add(localPath.toString());
                }
            }

            // Sort files by scene number
            audioFiles.sort(Comparator.naturalOrder());
            imageFiles.sort(Comparator.naturalOrder());

            System.out.println("Downloaded " + audioFiles.size() + " audio files and " +
                    imageFiles.size() + " image files");

            // Stitch into video
            stitchVideo(audioFiles, imageFiles, tempDir, outputPath);

            System.out.println("Video created successfully: " + outputPath);

        } finally {
            // Clean up temporary directory
            deleteDirectory(tempDir);
        }
    }

    /**
     * Download a file from S3
     */
    private void downloadFile(String s3Key, Path localPath) throws IOException {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getRequest);
        Files.write(localPath, objectBytes.asByteArray());
    }

    /**
     * Use ffmpeg to stitch audio and images into MP4
     */
    private void stitchVideo(List<String> audioFiles, List<String> imageFiles,
                             Path tempDir, Path outputPath) throws Exception {

        if (audioFiles.isEmpty() || imageFiles.isEmpty()) {
            throw new IllegalStateException("No audio or image files found to stitch");
        }

        if (audioFiles.size() != imageFiles.size()) {
            throw new IllegalStateException("Mismatch: " + audioFiles.size() +
                    " audio files but " + imageFiles.size() + " image files");
        }

        // Get duration of each audio file
        List<Double> audioDurations = new ArrayList<>();
        for (String audioFile : audioFiles) {
            double duration = getAudioDuration(audioFile);
            audioDurations.add(duration);
            System.out.println("Audio duration: " + duration + "s for " + audioFile);
        }

        // Create a concat file for images with proper durations
        Path imageListFile = tempDir.resolve("images.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(imageListFile)) {
            for (int i = 0; i < imageFiles.size(); i++) {
                writer.write("file '" + imageFiles.get(i) + "'\n");
                writer.write("duration " + audioDurations.get(i) + "\n");
            }
            // Repeat last image (ffmpeg concat requirement)
            writer.write("file '" + imageFiles.get(imageFiles.size() - 1) + "'\n");
        }

        // Concatenate all audio files
        Path combinedAudioFile = tempDir.resolve("combined_audio.mp3");
        Path audioListFile = tempDir.resolve("audio.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(audioListFile)) {
            for (String audioFile : audioFiles) {
                writer.write("file '" + audioFile + "'\n");
            }
        }

        // Step 1: Concatenate audio files
        System.out.println("Concatenating audio files...");
        ProcessBuilder audioConcat = new ProcessBuilder(
                "ffmpeg",
                "-f", "concat",
                "-safe", "0",
                "-i", audioListFile.toString(),
                "-c", "copy",
                combinedAudioFile.toString()
        );
        audioConcat.redirectErrorStream(true);
        runProcess(audioConcat, "Audio concatenation");

        // Step 2: Create video from images with combined audio
        System.out.println("Creating video from images...");
        ProcessBuilder videoBuilder = new ProcessBuilder(
                "ffmpeg",
                "-f", "concat",
                "-safe", "0",
                "-i", imageListFile.toString(),
                "-i", combinedAudioFile.toString(),
                "-vsync", "cfr",
                "-r", "1",  // 1 frame per second (since images are static)
                "-pix_fmt", "yuv420p",
                "-vf", "scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2",
                "-c:v", "libx264",
                "-c:a", "aac",
                "-y", // Overwrite output file
                outputPath.toString()
        );
        videoBuilder.redirectErrorStream(true);
        runProcess(videoBuilder, "Video creation");
    }

    /**
     * Get the duration of an audio file in seconds using ffprobe
     */
    private double getAudioDuration(String audioFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                audioFile
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("ffprobe failed with exit code: " + exitCode);
        }

        return Double.parseDouble(output.toString().trim());
    }

    /**
     * Run a process and capture output
     */
    private void runProcess(ProcessBuilder pb, String processName) throws Exception {
        System.out.println("Running: " + processName);
        Process process = pb.start();

        // Capture output
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException(processName + " failed with exit code: " + exitCode);
        }
        System.out.println(processName + " completed successfully");
    }

    /**
     * Recursively delete a directory
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Failed to delete: " + path);
                        }
                    });
        }
    }
}