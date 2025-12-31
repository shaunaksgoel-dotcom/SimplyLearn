package com.example.simplylearn.controller;

import com.example.simplylearn.model.FileUpload;
import com.example.simplylearn.repository.FileUploadRepository;
import com.example.simplylearn.service.ConversionService;
import com.example.simplylearn.service.StorageService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping({"/"})
    public String home(Model model) {
        List<FileUpload> files = this.repo.findAll();
        model.addAttribute("files", files);
        return "home";
    }

    @GetMapping({"/new"})
    public String uploadPage() {
        return "new";
    }

    @PostMapping({"/upload"})
    public String upload(@RequestParam("files") MultipartFile[] files, @RequestParam("conversion") String conversion) throws Exception {
        if (files == null || files.length == 0)
            return "redirect:/new";
        List<String> storedFilenames = new ArrayList<>();
        List<String> originalFilenames = new ArrayList<>();
        for (MultipartFile f : files) {
            String stored = this.storageService.store(f);
            storedFilenames.add(stored);
            originalFilenames.add(f.getOriginalFilename());
        }
        FileUpload upload = new FileUpload();
        upload.setOriginalFilename(String.join(", ", (Iterable)originalFilenames));
        upload.setStoredFilename(String.join("|", (Iterable)storedFilenames));
        upload.setConversionType(conversion);
        upload.setStatus("UPLOADED");
        upload.setUploadedAt(Instant.now());
        upload = (FileUpload)this.repo.save(upload);
        this.conversionService.convert(upload.getId());
        return "redirect:/";
    }

    @GetMapping({"/quiz/{id}"})
    public String quizPage(@PathVariable UUID id, Model attributes) throws Exception {
        FileUpload upload = repo.findById(id).orElseThrow();

        Path quizFile = storageService.resolveConverted(
                upload.getConvertedFilename()
        );

        String quizData = Files.readString(quizFile);
        quizData = quizData.replaceAll("(?s)```(json)?", "").trim();


//        // Inject JSON directly into page
        attributes.addAttribute("quizData",quizData);
        return "quiz";
    }


    @GetMapping({"/download/{id}"})
    public ResponseEntity<FileSystemResource> download(@PathVariable UUID id) {
        String filename, contentType;
        FileUpload upload = (FileUpload)this.repo.findById(id).orElseThrow();
        Path file = this.storageService.resolveConverted(upload.getConvertedFilename());
        switch (upload.getConversionType().toLowerCase()) {
            case "podcast":
                filename = "podcast.mp3";
                contentType = "audio/mpeg";
                break;
            case "slideshow":
                filename = "slideshow.pptx";
                contentType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                break;
            case "summary":
                filename = "summary.txt";
                contentType = "text/plain";
                break;
            case "quiz":
                filename = "quiz.json";
                contentType = "application/json";
                break;
            default:
                filename = upload.getConvertedFilename();
                contentType = "application/octet-stream";
                break;
        }
        return ((ResponseEntity.BodyBuilder)((ResponseEntity.BodyBuilder)ResponseEntity.ok()
                .header("Content-Disposition", new String[] { "attachment; filename=\"" + filename + "\"" })).header("Content-Type", new String[] { contentType })).body(new FileSystemResource(file));
    }
}

