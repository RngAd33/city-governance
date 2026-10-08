package com.cityplatform.govai.repository;

import com.cityplatform.govai.model.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 政务报告数据访问层
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * 全部报告按创建时间倒序（最新在前）
     */
    List<Report> findAllByOrderByCreatedAtDesc();

    /**
     * 指定类型的报告按创建时间倒序
     */
    List<Report> findByTypeOrderByCreatedAtDesc(String type);
}
