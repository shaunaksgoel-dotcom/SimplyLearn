package com.example.simplylearn.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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

    private String conversionType;

    private String status;

    private Instant uploadedAt;

    public UUID getId() {
        return this.id;
    }

    public String getOriginalFilename() {
        return this.originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return this.storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getConvertedFilename() {
        return this.convertedFilename;
    }

    public void setConvertedFilename(String convertedFilename) {
        this.convertedFilename = convertedFilename;
    }

    public String getConversionType() {
        return this.conversionType;
    }

    public void setConversionType(String conversionType) {
        this.conversionType = conversionType;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getUploadedAt() {
        return this.uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
