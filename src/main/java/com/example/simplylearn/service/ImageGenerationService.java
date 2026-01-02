package com.example.simplylearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Service
public class ImageGenerationService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final S3Client s3Client;
    private final String openAiApiKey;
    private final String bucketName;

    public ImageGenerationService(
            @Value("${openai.api.key}") String openAiApiKey,
            @Value("${aws.s3.bucket.name:simplylearn-video-scenes}") String bucketName) {
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
        this.openAiApiKey = openAiApiKey;
        this.bucketName = bucketName;

        // Initialize S3 client (uses IAM role when deployed to EBS)
        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    @PreDestroy
    public void close() {
        s3Client.close();
    }

    /**
     * Parse the OpenAI JSON response and generate images for each scene
     * Saves directly to S3
     * @param openAiJsonResponse The JSON response from OpenAI
     * @param uploadId The upload ID (used as S3 folder prefix)
     */
    public void generateImagesFromScenes(String openAiJsonResponse, String uploadId) throws Exception {
        // Parse the JSON response
        JsonNode rootNode = objectMapper.readTree(openAiJsonResponse);
        JsonNode scenesArray = rootNode.get("scenes");

        if (scenesArray == null || !scenesArray.isArray()) {
            throw new IllegalArgumentException("Invalid JSON: 'scenes' array not found");
        }

        // Process each scene
        for (JsonNode sceneNode : scenesArray) {
            int sceneNumber = sceneNode.get("sceneNumber").asInt();
            String illustrationIdea = sceneNode.get("illustrationIdea").asText();

            // Create S3 key (path in bucket)
            String s3Key = uploadId + "/" + String.format("imageScene%04d.png", sceneNumber);

            System.out.println("Generating image for Scene " + sceneNumber + "...");

            // Generate and upload the image to S3
            generateImageAndUpload(illustrationIdea, s3Key);
        }
    }

    /**
     * Generate a single image using DALL-E and upload directly to S3
     */
    private void generateImageAndUpload(String prompt, String s3Key) throws Exception {
        String dalleUrl = "https://api.openai.com/v1/images/generations";

        // Prepare request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "dall-e-3");
        requestBody.put("prompt", prompt);
        requestBody.put("n", 1);
        requestBody.put("size", "1024x1024");
        requestBody.put("quality", "standard");

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + openAiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Make API call to DALL-E
        ResponseEntity<String> response = restTemplate.exchange(
                dalleUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        // Get image URL from response
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        String imageUrl = responseJson.get("data").get(0).get("url").asText();

        // Download image from DALL-E URL
        byte[] imageBytes;
        try (InputStream in = new URL(imageUrl).openStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(data)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            imageBytes = buffer.toByteArray();
        }

        // Upload to S3
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType("image/png")
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(imageBytes));

        System.out.println("Uploaded image to S3: s3://" + bucketName + "/" + s3Key);
    }

    // =====================================================
    // NEW METHOD FOR SLIDESHOW IMAGE GENERATION
    // =====================================================

    /**
     * Generate a single image using DALL-E and return the image bytes
     * This method is useful for embedding images directly (e.g., in PowerPoint)
     * @param prompt The image description/prompt
     * @return byte array of the generated PNG image
     */
    public byte[] generateSingleImage(String prompt) throws Exception {
        String dalleUrl = "https://api.openai.com/v1/images/generations";

        // Prepare request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "dall-e-3");
        requestBody.put("prompt", prompt);
        requestBody.put("n", 1);
        requestBody.put("size", "1024x1024");
        requestBody.put("quality", "standard");

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + openAiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Make API call to DALL-E
        ResponseEntity<String> response = restTemplate.exchange(
                dalleUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        // Get image URL from response
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        String imageUrl = responseJson.get("data").get(0).get("url").asText();

        // Download image from DALL-E URL and return bytes
        try (InputStream in = new URL(imageUrl).openStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(data)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            return buffer.toByteArray();
        }
    }
}