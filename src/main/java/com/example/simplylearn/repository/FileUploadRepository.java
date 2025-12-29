package com.example.simplylearn.repository;

import com.example.simplylearn.model.FileUpload;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileUploadRepository extends JpaRepository<FileUpload, UUID> {}
