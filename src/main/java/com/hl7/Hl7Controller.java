package com.hl7;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/hl7")
public class Hl7Controller {

    @Autowired
    private CsvToHl7Converter hl7ConverterService;

    // Define the folder where files will be saved relative to where the app runs
    private static final String UPLOAD_DIR = "uploads/";

    /**
     * API 1: Upload a CSV file and save it to the server.
     * POST /api/hl7/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a valid CSV file.");
        }

        try {
            // 1. Create the "uploads" directory if it doesn't exist yet
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 2. Save the file locally using its original name
            String fileName = file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.write(path, file.getBytes());

            return ResponseEntity.ok("File uploaded successfully. You can now convert it using filename: " + fileName);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save file to server: " + e.getMessage());
        }
    }

    /**
     * API 2: Read the saved file and convert to HL7 format.
     * GET /api/hl7/convert/{fileName}
     */
    @GetMapping("/convert/{fileName}")
    public ResponseEntity<?> convertSavedFile(@PathVariable String fileName) {
        try {
            // Locate the file in our uploads folder
            String filePath = UPLOAD_DIR + fileName;

            // Call the new service method we just created
            List<String> messages = hl7ConverterService.processExternalFile(filePath);
            return ResponseEntity.ok(messages);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        }
    }
}