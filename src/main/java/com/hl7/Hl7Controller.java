package com.hl7;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/hl7")
public class Hl7Controller {

    @Autowired
    private CsvToHl7Converter hl7ConverterService;

    /**
     * Endpoint to convert the default patients.csv located in the resources folder.
     * GET /api/hl7/convert-default
     */
    @GetMapping("/convert-default")
    public ResponseEntity<?> convertDefaultCsv() {
        try {
            List<String> messages = hl7ConverterService.processCsvResource("patients.csv");
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        }
    }

    /**
     * Endpoint to upload any custom CSV file and get HL7 messages in return.
     * POST /api/hl7/upload-csv
     */
    @PostMapping("/upload-csv")
    public ResponseEntity<?> uploadCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a valid CSV file.");
        }

        try {
            List<String> messages = hl7ConverterService.processCsvUpload(file);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing uploaded file: " + e.getMessage());
        }
    }

    @GetMapping("/data")
    public String getData(String data){
        return data = "my love india";
    }
}
