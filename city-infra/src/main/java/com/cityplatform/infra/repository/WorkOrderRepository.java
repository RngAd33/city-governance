package com.cityplatform.infra.repository;

import com.cityplatform.infra.model.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 运维工单数据访问层
 */
@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    /**
     * 全部工单按创建时间倒序（最新在前）
     */
    List<WorkOrder> findAllByOrderByCreatedAtDesc();
}