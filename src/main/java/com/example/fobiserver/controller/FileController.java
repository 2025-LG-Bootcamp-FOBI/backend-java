package com.example.fobiserver.controller;

import com.example.fobiserver.service.FileService;
import lombok.RequiredArgsConstructor;
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
        String savedPath = fileService.saveFile(file);

        return ResponseEntity.ok("File saved to: " + savedPath);
    }

    @GetMapping("/files")
    public ResponseEntity<List<String>> fileList() {
        try (Stream<Path> paths = Files.list(Paths.get(FOLDER_PATH))) {
            List<String> body = paths
                    .filter(Files::isRegularFile) // 파일만 필터링
                    .map(Path::toString) // 파일 경로를 문자열로 변환
                    .map(path -> path.split("/shared/")[1])
                    .collect(Collectors.toList());
            return ResponseEntity.ok(body);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
