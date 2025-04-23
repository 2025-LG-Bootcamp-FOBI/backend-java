package com.example.fobiserver.model.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Builder
@Document(indexName = "keywords")
public class KeywordDocument {

    @Id
    private String id;

    @Field(type = FieldType.Integer, index = false, docValues = false)
    private int level;

    @Field(type = FieldType.Integer, index = false, docValues = false)
    private int page;

    @Field(type = FieldType.Text, docValues = false)
    private String title;

    @Field(type = FieldType.Object, index = false, docValues = false)
    private List<String> subKeywords;

    @Field(type = FieldType.Keyword, index = false, docValues = false)
    private String parentId;

    @Field(type = FieldType.Text, index = false, docValues = false)
    private String fileName;

    @Field(type = FieldType.Object, index = false, docValues = false)
    private List<Person>  persons;

    @Field(type = FieldType.Object, index = false, docValues = false)
    private List<Issue> issues;
}
