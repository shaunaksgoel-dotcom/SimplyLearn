package com.example.simplylearn.controller;

import com.example.simplylearn.model.ConversionType;
import com.example.simplylearn.model.FileUpload;
import com.example.simplylearn.repository.FileUploadRepository;
import com.example.simplylearn.service.ConversionService;
import com.example.simplylearn.service.StorageService;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Controller
public class FileController {

    private final StorageService storageService;
    private final FileUploadRepository repo;
    private final ConversionService conversionService;

    public FileController(
            StorageService storageService,
            FileUploadRepository repo,
            ConversionService conversionService
    ) {
        this.storageService = storageService;
        this.repo = repo;
        this.conversionService = conversionService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("files", repo.findAll());
        return "home";
    }

    @GetMapping("/new")
    public String uploadPage() {
        return "new";
    }

    @PostMapping("/upload")
    public String upload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("conversion") String conversion
    ) throws Exception {

        log.info("Upload request received with conversion: " + conversion);
        log.error("Upload request received with conversion: " + conversion);
        System.out.println("Upload request received with conversion: " + conversion);

        List<String> stored = new ArrayList<>();
        List<String> originals = new ArrayList<>();

        for (MultipartFile f : files) {
            stored.add(storageService.store(f));
            originals.add(f.getOriginalFilename());
        }

        FileUpload upload = new FileUpload();
        upload.setOriginalFilename(String.join(", ", originals));
        upload.setStoredFilename(String.join("|", stored));

        // ✅ SAFE ENUM CONVERSION
        upload.setConversionType(
                ConversionType.valueOf(conversion.toUpperCase())
        );

        upload.setStatus("UPLOADED");
        upload.setUploadedAt(Instant.now());

        repo.save(upload);
        conversionService.convert(upload.getId());

        return "redirect:/";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<FileSystemResource> download(@PathVariable UUID id) {
        log.error("Download request for ID: " + id);
        log.info("Download request for ID: " + id);
        System.out.println("Download request for ID: " + id);
        FileUpload upload = repo.findById(id).orElseThrow();
        var file = storageService.resolveConverted(upload.getConvertedFilename());

        String filename;
        String contentType;

        switch (upload.getConversionType()) {
            case PODCAST -> {
                filename = "podcast.mp3";
                contentType = "audio/mpeg";
            }
            case SLIDESHOW -> {
                filename = "slideshow.pptx";
                contentType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            }
            case SUMMARY -> {
                filename = "summary.txt";
                contentType = "text/plain";
            }
            case VIDEO -> {
                filename = "video.mp4";
                contentType = "video/mp4";
            }
            default -> {
                filename = upload.getConvertedFilename();
                contentType = "application/octet-stream";
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(new FileSystemResource(file));
    }
}
