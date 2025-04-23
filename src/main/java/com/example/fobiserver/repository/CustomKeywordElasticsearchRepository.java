package com.example.fobiserver.repository;

import com.example.fobiserver.model.entity.KeywordDocument;

import java.util.List;

public interface CustomKeywordElasticsearchRepository {
    List<KeywordDocument> searchByFileNameAndTitle(String fileName, String keyword);
}
