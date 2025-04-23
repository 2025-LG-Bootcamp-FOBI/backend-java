package com.example.fobiserver.repository;

import com.example.fobiserver.model.entity.KeywordDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Repository
public class CustomKeywordElasticsearchRepositoryImpl implements CustomKeywordElasticsearchRepository {

    private final ElasticsearchTemplate elasticsearchTemplate;

    @Override
    public List<KeywordDocument> searchByFileNameAndTitle(String fileName, String keyword) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .must(m1 -> m1.match(m -> m.field("fileName").query(fileName)))
                        .must(m2 -> m2.matchPhrasePrefix(mp -> mp.field("title").query(keyword)))
                ))
                .build();

        log.info("searchByFileNameAndTitle Query: {}", query.getQuery());

        return elasticsearchTemplate.search(query, KeywordDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }
}
