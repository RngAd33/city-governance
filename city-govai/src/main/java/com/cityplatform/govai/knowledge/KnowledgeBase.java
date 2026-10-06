package com.cityplatform.govai.knowledge;

import com.cityplatform.govai.repository.KnowledgeItemRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 政务知识库（RAG知识源）
 * 知识条目持久化于 MySQL knowledge_items 表，启动时校验可用性；
 * 检索基于「标题/关键词字段/分类/正文」多级命中 + 优先级加权，按相关度倒序返回
 */
@Component
@Slf4j
public class KnowledgeBase {

    /** 检索返回的最大条数 */
    private static final int MAX_RESULTS = 10;

    @Resource
    private KnowledgeItemRepository repository;

    @PostConstruct
    public void init() {
        try {
            long count = repository.countByEnabledTrue();
            log.info("政务知识库(MySQL knowledge_items)初始化完成，启用条目 {} 条", count);
            if (count == 0) {
                log.warn("知识库暂无启用条目，请执行 sql/seed_govai_knowledge_chat.sql 初始化知识种子");
            }
        } catch (Exception e) {
            log.warn("政务知识库加载失败: {}", e.getMessage());
        }
    }

    /**
     * 检索知识库：多级关键词命中 + 优先级加权，按相关度倒序，最多返回 10 条
     */
    public List<KnowledgeItem> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> keywords = tokenize(query);

        return repository.findByEnabledTrueOrderByPriorityDescIdAsc().stream()
                .map(item -> {
                    item.setRelevanceScore(calculateRelevance(item, keywords));
                    return item;
                })
                .filter(item -> item.getRelevanceScore() > 0)
                .sorted(Comparator.comparingDouble(KnowledgeItem::getRelevanceScore).reversed())
                .limit(MAX_RESULTS)
                .collect(Collectors.toList());
    }

    /**
     * 按类别获取知识条目（忽略大小写）
     */
    public List<KnowledgeItem> getByCategory(String category) {
        return repository.findByEnabledTrueAndCategoryIgnoreCaseOrderByPriorityDescIdAsc(category);
    }

    /**
     * 获取全部启用知识条目（优先级倒序）
     */
    public List<KnowledgeItem> getAll() {
        return new ArrayList<>(repository.findByEnabledTrueOrderByPriorityDescIdAsc());
    }

    /**
     * 相关度计算：标题权重3、关键词字段权重2.5、分类权重2、正文权重1；
     * 仅在有关键词命中时加入优先级(0-10)微调，避免无命中条目混入结果
     */
    private double calculateRelevance(KnowledgeItem item, List<String> keywords) {
        String title = lower(item.getTitle());
        String content = lower(item.getContent());
        String category = lower(item.getCategory());
        String keywordField = lower(item.getKeywords());

        double score = 0;
        for (String keyword : keywords) {
            if (keyword.isEmpty()) {
                continue;
            }
            if (title.contains(keyword)) score += 3;
            if (keywordField.contains(keyword)) score += 2.5;
            if (category.contains(keyword)) score += 2;
            if (content.contains(keyword)) score += 1;
        }
        if (score <= 0) {
            return 0;
        }
        return score + (item.getPriority() == null ? 0 : item.getPriority()) / 10.0;
    }

    /**
     * 轻量分词：按标点/空白切分 + 中文长片段二元切分，
     * 保证「城市内涝应急预案有哪些关键措施」这类无空格整句也能召回相关条目
     */
    private List<String> tokenize(String query) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String piece : query.toLowerCase()
                .split("[\\s,，。；;：:、（）()/？?！!\"'“”【】\\[\\]]+")) {
            if (piece.isEmpty()) {
                continue;
            }
            tokens.add(piece);
            if (piece.length() >= 4 && hasCjk(piece)) {
                for (int i = 0; i + 2 <= piece.length(); i++) {
                    tokens.add(piece.substring(i, i + 2));
                }
            }
        }
        return new ArrayList<>(tokens);
    }

    /**
     * 是否包含中日韩统一表意文字
     */
    private boolean hasCjk(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.UnicodeBlock.of(value.charAt(i)) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                return true;
            }
        }
        return false;
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

}
