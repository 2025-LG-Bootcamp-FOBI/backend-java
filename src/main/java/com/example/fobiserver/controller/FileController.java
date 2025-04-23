package com.example.fobiserver.controller;

import com.example.fobiserver.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.fobiserver.constant.Constant.FOLDER_PATH;

@RequiredArgsConstructor
@RestController
public class FileController {

    private final FileService fileService;

    @PostMapping("/files")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getSize() == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("file is empty");
        }
        fileService.saveFile(file);
        return ResponseEntity.ok(file.getOriginalFilename());
    }

    @GetMapping("/files")
    public ResponseEntity<List<String>> fileList() {
        return ResponseEntity.ok(fileService.listFiles());
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> getFile(@RequestParam String fileName) {
        return ResponseEntity.ok(fileService.getFile(fileName));
    }

    @PutMapping("/files")
    public ResponseEntity<String> updateFile(@RequestParam String oldName, String newName) {
        fileService.updateFile(oldName, newName);
        return ResponseEntity.ok("file name was successfully updated");
    }

    @DeleteMapping("/files")
    public ResponseEntity<String> deleteFile(@RequestParam String fileName) {
        fileService.deleteFile(fileName);
        return ResponseEntity.ok("file was successfully deleted");
    }
}
