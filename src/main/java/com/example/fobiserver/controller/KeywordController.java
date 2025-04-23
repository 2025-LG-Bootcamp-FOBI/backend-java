package com.example.fobiserver.controller;

import com.example.fobiserver.model.entity.KeywordDocument;
import com.example.fobiserver.service.KeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class KeywordController {

    private final KeywordService keywordService;

    @GetMapping("/search")
    public ResponseEntity<List<KeywordDocument>> search(@RequestParam String fileName, @RequestParam(required = false) String keyword) {
        List<KeywordDocument> keywordDocuments = keywordService.getKeywords(fileName, keyword);
        return ResponseEntity.ok(keywordDocuments);
    }

}
