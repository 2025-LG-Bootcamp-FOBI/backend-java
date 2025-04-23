package com.example.fobiserver.service;

import com.example.fobiserver.model.entity.Issue;
import com.example.fobiserver.model.entity.KeywordDocument;
import com.example.fobiserver.model.entity.Person;
import com.example.fobiserver.repository.CustomKeywordElasticsearchRepository;
import com.example.fobiserver.repository.IssueRepository;
import com.example.fobiserver.repository.KeywordElasticsearchRepository;
import com.example.fobiserver.repository.PersonRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class KeywordService {
    private final KeywordElasticsearchRepository keywordRepository;
    private final CustomKeywordElasticsearchRepository customKeywordRepository;
    private final PersonRepository personRepository;
    private final IssueRepository issueRepository;

    private final Random random = new Random();

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
                    .issues(getRandomIssues())
                    .persons(getRandomPersons())
                    .build();

            documents.add(doc);
        }

        log.info("inserted {} documents", documents.size());

        keywordRepository.saveAll(documents);
    }

    public void updateKeyword(String oldName, JsonNode root) {
        saveKeywordsFromJson(root);
        deleteKeyword(oldName);
    }

    public void deleteKeyword(String fileName) {
        List<KeywordDocument> documents = keywordRepository.findByFileName(fileName);
        if (documents.isEmpty()) {
            return;
        }
        keywordRepository.deleteAll(documents);
        log.info("deleted old {} documents", documents.size());
    }

    private List<Person> getRandomPersons() {
        List<Person> allPersons = personRepository.findAll();
        Collections.shuffle(allPersons);
        int count = 1 + random.nextInt(3); // 1 ~ 3명
        return allPersons.stream().limit(count).collect(Collectors.toList());
    }

    private List<Issue> getRandomIssues() {
        List<Issue> allIssues = issueRepository.findAll();
        Collections.shuffle(allIssues);
        int count = 1 + random.nextInt(2); // 1 ~ 2개
        return allIssues.stream().limit(count).collect(Collectors.toList());
    }
}
