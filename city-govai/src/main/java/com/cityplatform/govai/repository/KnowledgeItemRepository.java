package com.cityplatform.govai.repository;

import com.cityplatform.govai.knowledge.KnowledgeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 政务知识库数据访问层（knowledge_items 表）
 */
@Repository
public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItem, String> {

    /**
     * 全部启用条目，按优先级倒序
     */
    List<KnowledgeItem> findByEnabledTrueOrderByPriorityDescIdAsc();

    /**
     * 按分类查询启用条目（忽略大小写），按优先级倒序
     */
    List<KnowledgeItem> findByEnabledTrueAndCategoryIgnoreCaseOrderByPriorityDescIdAsc(String category);

    /**
     * 启用条目数量
     */
    long countByEnabledTrue();
}
