package com.example.fobiserver.service;

import com.example.fobiserver.util.PythonExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static com.example.fobiserver.constant.Constant.FOLDER_PATH;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileService {

    private final PythonExecutor pythonExecutor;
    private final ObjectMapper objectMapper;

    private final KeywordService keywordService;

    public void saveFile(MultipartFile file) {
        String filePath = FOLDER_PATH + File.separator + file.getOriginalFilename();
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
}
