package com.example.fobiserver.service;

import com.example.fobiserver.util.PythonExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    public void saveFile(MultipartFile file) {

        String fileName = file.getOriginalFilename();

        // 동일 파일 이름 존재일 경우 _숫자 추가
        int count =  countFileNames(file.getOriginalFilename());
        if (count > 0) {
            fileName = fileName + "_" + count;
        }

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
                    .map(Path::toString) // 파일 경로를 문자열로 변환
                    .map(path -> path.split("/")[1])
                    .toList();
        } catch (IOException e) {
            log.error("File list error", e);
            return new ArrayList<>();
        }
    }

    private int countFileNames(String fileName) {
        return (int) listFiles()
                .stream()
                .filter(file -> file.equals(fileName))
                .count();
    }
}
