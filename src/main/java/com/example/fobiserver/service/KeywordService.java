package com.example.fobiserver.service;

import com.example.fobiserver.model.entity.KeywordDocument;
import com.example.fobiserver.repository.CustomKeywordElasticsearchRepository;
import com.example.fobiserver.repository.KeywordElasticsearchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class KeywordService {
    private final KeywordElasticsearchRepository keywordRepository;
    private final CustomKeywordElasticsearchRepository customKeywordRepository;

    public List<KeywordDocument> getKeywords(String fileName, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return keywordRepository.findByFileName(fileName);
        }
        return customKeywordRepository.searchByFileNameAndTitle(fileName, keyword);
    }

    public void saveKeywordsFromJson(JsonNode root) {
        List<KeywordDocument> documents = new ArrayList<>();

        for (JsonNode item : root) {
            List<String> subKeywords = new ArrayList<>();
            JsonNode subKeywordsNode = item.get("subKeywords");
            if (subKeywordsNode != null && subKeywordsNode.isArray()) {
                for (JsonNode keywordNode : subKeywordsNode) {
                    subKeywords.add(keywordNode.asText());
                }
            }

            KeywordDocument doc = KeywordDocument.builder()
                    .id(item.get("id").asText())
                    .level(item.get("level").asInt())
                    .page(item.get("page").asInt())
                    .title(item.get("title").asText())
                    .subKeywords(subKeywords)
                    .parentId(item.hasNonNull("parentId") ? item.get("parentId").asText() : null)
                    .fileName(item.get("fileName").asText())
                    .build();

            documents.add(doc);
        }

        log.info("inserted {} documents", documents.size());

        keywordRepository.saveAll(documents);
    }
}
