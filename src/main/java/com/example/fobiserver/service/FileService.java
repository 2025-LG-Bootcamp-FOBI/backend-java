package com.example.fobiserver.service;

import com.example.fobiserver.util.PythonExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.example.fobiserver.constant.Constant.FOLDER_PATH;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileService {

    private final PythonExecutor pythonExecutor;
    private final ObjectMapper objectMapper;

    private final KeywordService keywordService;

    private final String PDF_EXTENSION = ".pdf";

    public void saveFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return;
        }

        String fileName = setFileName(file.getOriginalFilename());
        String filePath = FOLDER_PATH + File.separator + fileName;
        Path uploadPath = Paths.get(filePath);

        log.info("filePath: {}", filePath);

        // 파일 저장 및 Elasticsearch 키워드 저장
        try {
            // 디렉토리 없으면 생성
            if (!Files.exists(uploadPath.getParent())) {
                Files.createDirectories(uploadPath.getParent());
            }

            Files.copy(file.getInputStream(), uploadPath, StandardCopyOption.REPLACE_EXISTING);
            String keywords = pythonExecutor.executePythonScript(filePath, new String[0]);
            log.info("keywords: {}", keywords);
            keywordService.saveKeywordsFromJson(objectMapper.readTree(keywords));
        } catch (Exception e) {
            log.error("File save or parse error", e);
        }
    }

    public List<String> listFiles() {
        try (Stream<Path> paths = Files.list(Paths.get(FOLDER_PATH))) {
            return paths.filter(Files::isRegularFile)
                    .sorted((p1, p2) -> {
                        try {
                            FileTime t1 = Files.getLastModifiedTime(p1);
                            FileTime t2 = Files.getLastModifiedTime(p2);
                            return t2.compareTo(t1); // 최신 순으로 정렬 (내림차순)
                        } catch (IOException e) {
                            return 0; // 에러 시 정렬에 영향 없도록
                        }
                    })
                    .map(Path::toString) // 파일 경로를 문자열로 변환
                    .map(path -> path.split("/")[1])
                    .toList();
        } catch (IOException e) {
            log.error("File list error", e);
            return new ArrayList<>();
        }
    }

    public Resource getFile(String fileName) {
        String filePath = FOLDER_PATH + File.separator + fileName;
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            return null;
        }

        return new FileSystemResource(file);
    }

    public void updateFile(String oldName, String newName) {

        if (oldName == null || newName == null
        || oldName.isBlank() || newName.isBlank()) {
            return;
        }

        if (checkDuplication(newName)) {
            return;
        }

        String oldFilePath = FOLDER_PATH + File.separator + oldName;
        String newFilePath = FOLDER_PATH + File.separator + newName;

        File oldFile = new File(oldFilePath);
        File newFile = new File(newFilePath);

        boolean renamed = oldFile.renameTo(newFile);
        if (!renamed) {
            log.error("Failed to rename file from {} to {}", oldName, newName);
            return;
        }

        log.info("File renamed from {} to {}", oldName, newName);

        try {
            String keywords = pythonExecutor.executePythonScript(newFilePath, new String[0]);
            log.info("keywords: {}", keywords);
            keywordService.updateKeyword(oldName, objectMapper.readTree(keywords));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void deleteFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }

        String filePath = FOLDER_PATH + File.separator + fileName;
        File file = new File(filePath);

        if (!file.exists()) {
            log.warn("File not found: {}", filePath);
            return;
        }

        boolean deleted = file.delete();

        if (deleted) {
            keywordService.deleteKeyword(fileName);
            log.info("File deleted: {}", filePath);
        } else {
            log.error("Failed to delete file: {}", filePath);
        }
    }

    private boolean checkDuplication(String fileName) {
        return listFiles().contains(fileName);
    }

    private String setFileName(String fileName) {
        String name = fileName.split(PDF_EXTENSION)[0];

        int count = (int) listFiles()
                .stream()
                .filter(file -> file.contains(name))
                .count();

        if (count > 0) {
            return name + "_" + count + PDF_EXTENSION;
        } else {
            return fileName;
        }
    }
}
