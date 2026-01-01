package com.example.simplylearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.*;

@Service
public class AudioService {

    private final ObjectMapper objectMapper;
    private final PollyClient pollyClient;
    private final S3Client s3Client;
    private final String bucketName;

    public AudioService(@Value("${aws.s3.bucket.name:simplylearn-video-scenes}") String bucketName) {
        this.objectMapper = new ObjectMapper();
        this.bucketName = bucketName;

        // Initialize AWS Polly client (uses IAM role when deployed to EBS)
        this.pollyClient = PollyClient.builder()
                .region(Region.US_EAST_1)  // REQUIRED for generative voices
                .build();

        // Initialize S3 client (uses IAM role when deployed to EBS)
        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    @PreDestroy
    public void close() {
        pollyClient.close();
        s3Client.close();
    }

    /**
     * Parse the OpenAI JSON response and generate audio files for each scene
     * Saves directly to S3
     * @param openAiJsonResponse The JSON response from OpenAI
     * @param uploadId The upload ID (used as S3 folder prefix)
     */
    public void generateAudioFromScenes(String openAiJsonResponse, String uploadId) throws Exception {
        // Parse the JSON response
        JsonNode rootNode = objectMapper.readTree(openAiJsonResponse);
        JsonNode scenesArray = rootNode.get("scenes");

        if (scenesArray == null || !scenesArray.isArray()) {
            throw new IllegalArgumentException("Invalid JSON: 'scenes' array not found");
        }

        // Process each scene
        for (JsonNode sceneNode : scenesArray) {
            int sceneNumber = sceneNode.get("sceneNumber").asInt();
            String script = sceneNode.get("script").asText();

            // Create S3 key (path in bucket)
            String s3Key = uploadId + "/" + String.format("audioScene%04d.mp3", sceneNumber);

            System.out.println("Generating audio for Scene " + sceneNumber + "...");

            // Generate and upload the audio to S3
            textToSpeechAndUpload(script, s3Key);
        }
    }

    /**
     * Convert text to speech using Amazon Polly and upload directly to S3
     */
    private void textToSpeechAndUpload(String text, String s3Key) throws IOException {
        // Generate speech with Polly
        SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                .text(text)
                .outputFormat(OutputFormat.MP3)
                .voiceId(VoiceId.MATTHEW)  // Generative voice
                .engine(Engine.GENERATIVE)  // Most human-like
                .build();

        ResponseInputStream<SynthesizeSpeechResponse> pollyResponse =
                pollyClient.synthesizeSpeech(request);

        // Read audio stream into byte array
        byte[] audioBytes;
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            pollyResponse.transferTo(buffer);
            audioBytes = buffer.toByteArray();
        }

        // Upload to S3
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType("audio/mpeg")
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(audioBytes));

        System.out.println("Uploaded audio to S3: s3://" + bucketName + "/" + s3Key);
    }
}