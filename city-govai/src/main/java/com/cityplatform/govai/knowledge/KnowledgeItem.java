package com.cityplatform.govai.knowledge;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 知识库条目
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeItem {

    private String id;
    private String category;
    private String title;
    private String content;
    private String source;
    private double relevanceScore;

    public KnowledgeItem() {}

    public KnowledgeItem(String id, String category, String title, String content, String source) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.content = content;
        this.source = source;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }

    @Override
    public String toString() {
        return "KnowledgeItem{" +
                "id='" + id + '\'' +
                ", category='" + category + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
