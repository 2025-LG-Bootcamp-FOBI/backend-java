package com.example.fobiserver.repository;

import com.example.fobiserver.model.entity.KeywordDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface KeywordElasticsearchRepository extends ElasticsearchRepository<KeywordDocument, String> {
    List<KeywordDocument> findByFileName(String fileName);
}
