package com.example.simplylearn.service;


import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {
    private final Path uploadDir = Path.of("/tmp/uploads", new String[0]);

    private final Path convertedDir = Path.of("/tmp/converted", new String[0]);

    public StorageService() throws Exception {
        Files.createDirectories(this.uploadDir, (FileAttribute<?>[])new FileAttribute[0]);
        Files.createDirectories(this.convertedDir, (FileAttribute<?>[])new FileAttribute[0]);
    }

    public String store(MultipartFile file) throws Exception {
        String extension = "";
        if (file.getOriginalFilename() != null && file.getOriginalFilename().contains("."))
            extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String filename = String.valueOf(UUID.randomUUID()) + String.valueOf(UUID.randomUUID());
        Files.copy(file.getInputStream(), this.uploadDir.resolve(filename), new CopyOption[] { StandardCopyOption.REPLACE_EXISTING });
        return filename;
    }

    public String joinStoredFilenames(List<String> storedNames) {
        return String.join("|", (Iterable)storedNames);
    }

    public List<String> splitStoredFilenames(String storedFieldValue) {
        if (storedFieldValue == null || storedFieldValue.isBlank())
            return List.of();
        return Arrays.asList(storedFieldValue.split("\\|"));
    }

    public Path resolve(String filename) {
        return this.uploadDir.resolve(filename);
    }

    public Path resolveConverted(String filename) {
        return this.convertedDir.resolve(filename);
    }

    public void storeConverted(String filename, String content) throws Exception {
        Path p = resolveConverted(filename);
        Files.createDirectories(p.getParent(), (FileAttribute<?>[])new FileAttribute[0]);
        Files.writeString(p, content, StandardCharsets.UTF_8, new OpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING });
    }

    public String readAsString(String storedFilename) throws Exception {
        Path p = resolve(storedFilename);
        return Files.readString(p, StandardCharsets.UTF_8);
    }

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
