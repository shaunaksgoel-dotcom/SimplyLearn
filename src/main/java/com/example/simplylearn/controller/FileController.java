package com.example.simplylearn.controller;

import com.example.simplylearn.model.FileUpload;
import com.example.simplylearn.repository.FileUploadRepository;
import com.example.simplylearn.service.ConversionService;
import com.example.simplylearn.service.StorageService;
import java.util.ArrayList;


import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Controller
public class FileController {

    private final StorageService storageService;
    private final FileUploadRepository repo;
    private final ConversionService conversionService;

    public FileController(StorageService storageService, FileUploadRepository repo, ConversionService conversionService) {
        this.storageService = storageService;
        this.repo = repo;
        this.conversionService = conversionService;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<FileUpload> files = repo.findAll();
        model.addAttribute("files", files);
        return "home";
    }

    @GetMapping("/new")
    public String uploadPage() {
        return "new";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("files") MultipartFile[] files,
                         @RequestParam("conversion") String conversion) throws Exception {

        if (files == null || files.length == 0) {
            return "redirect:/new";
        }

        // Store all files, keep their stored filenames
        List<String> storedFilenames = new ArrayList<>();
        List<String> originalFilenames = new ArrayList<>();

        for (MultipartFile f : files) {
            String stored = storageService.store(f);
            storedFilenames.add(stored);
            originalFilenames.add(f.getOriginalFilename());
        }

        FileUpload upload = new FileUpload();
        upload.setOriginalFilename(String.join(", ", originalFilenames)); // simple display
        upload.setStoredFilename(String.join("|", storedFilenames));      // pipe-separated list
        upload.setConversionType(conversion);
        upload.setStatus("UPLOADED");
        upload.setUploadedAt(Instant.now());

        upload = repo.save(upload);

        conversionService.convert(upload.getId());

        return "redirect:/";
    }



    @GetMapping("/download/{id}")
    public ResponseEntity<FileSystemResource> download(@PathVariable UUID id) {

        FileUpload upload = repo.findById(id).orElseThrow();

        var file = storageService.resolveConverted(upload.getConvertedFilename());

        String filename;
        String contentType;

        switch (upload.getConversionType().toLowerCase()) {
            case "podcast" -> {
                filename = "podcast.mp3";
                contentType = "audio/mpeg";
            }
            case "slideshow" -> {
                filename = "slideshow.pptx";
                contentType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            }
            case "summary" -> {
                filename = "summary.txt";
                contentType = "text/plain";
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
