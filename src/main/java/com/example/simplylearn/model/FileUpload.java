package com.example.simplylearn.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
public class FileUpload {

    @Id
    @GeneratedValue
    private UUID id;

    private String originalFilename;
    private String storedFilename;
    private String convertedFilename;

    private String conversionType; // VIDEO, PODCAST, TEXT, etc.
    private String status; // UPLOADED, PROCESSING, DONE
    private Instant uploadedAt;

    public UUID getId() { return id; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public String getConvertedFilename() { return convertedFilename; }
    public void setConvertedFilename(String convertedFilename) { this.convertedFilename = convertedFilename; }

    public String getConversionType() { return conversionType; }
    public void setConversionType(String conversionType) { this.conversionType = conversionType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}
