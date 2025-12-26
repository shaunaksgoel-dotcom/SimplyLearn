package com.example.simplylearn.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class StorageService {

    private final Path uploadDir = Path.of("/tmp/uploads");
    private final Path convertedDir = Path.of("/tmp/converted");
    private final Path tempDir = Path.of("/tmp/temp"); // ✅ NEW (for video / temp files)

    public StorageService() throws Exception {
        Files.createDirectories(uploadDir);
        Files.createDirectories(convertedDir);
        Files.createDirectories(tempDir); // ✅ NEW
    }

    // ======================
    // UPLOADS
    // ======================

    // Stores ONE file, returns stored filename
    public String store(MultipartFile file) throws Exception {
        String extension = "";

        if (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")) {
            extension = file.getOriginalFilename()
                    .substring(file.getOriginalFilename().lastIndexOf("."));
        }

        String filename = UUID.randomUUID() + extension;

        Files.copy(
                file.getInputStream(),
                uploadDir.resolve(filename),
                StandardCopyOption.REPLACE_EXISTING
        );

        return filename;
    }

    // ======================
    // MULTI-FILE HELPERS
    // ======================

    // For multi-file support, we store filenames as "file1|file2|file3"
    public String joinStoredFilenames(List<String> storedNames) {
        return String.join("|", storedNames);
    }

    public List<String> splitStoredFilenames(String storedFieldValue) {
        if (storedFieldValue == null || storedFieldValue.isBlank()) return List.of();
        return Arrays.asList(storedFieldValue.split("\\|"));
    }

    // ======================
    // PATH RESOLUTION
    // ======================

    public Path resolve(String filename) {
        return uploadDir.resolve(filename);
    }

    public Path resolveConverted(String filename) {
        return convertedDir.resolve(filename);
    }

    // ✅ NEW — temp directory for video / slides / images
    public Path createTempDirectory(String name) throws Exception {
        Path dir = tempDir.resolve(name);
        Files.createDirectories(dir);
        return dir;
    }

    // ======================
    // CONVERTED OUTPUT
    // ======================

    public void storeConverted(String filename, String content) throws Exception {
        Path p = resolveConverted(filename);
        Files.createDirectories(p.getParent());

        Files.writeString(
                p,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    // ======================
    // FILE READING
    // ======================

    // Read ONE uploaded file as String (basic text files)
    public String readAsString(String storedFilename) throws Exception {
        Path p = resolve(storedFilename);
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    // Read MANY files and combine into one big string
    public String readManyAsString(List<String> storedFilenames) throws Exception {

        StringBuilder sb = new StringBuilder();

        for (String f : storedFilenames) {
            sb.append("\n\n===== FILE: ").append(f).append(" =====\n\n");
            try {
                sb.append(readAsString(f));
            } catch (Exception ex) {
                sb.append("[Could not read file as plain text. We'll add real PDF/PPTX parsing next.]");
            }
        }

        return sb.toString();
    }
}
