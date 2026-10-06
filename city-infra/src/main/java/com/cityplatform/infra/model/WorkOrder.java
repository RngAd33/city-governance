package com.cityplatform.infra.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基础设施运维工单实体
 * 存储南京地区设施运维工单（work_orders 表）
 */
@Entity
@Data
@Table(name = "work_orders")
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 工单编号（业务唯一键，如 WO-NJ-2026-001） */
    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    /** 设施ID（bridge/tunnel/road/pipeline/power_grid） */
    @Column(name = "facility_id", nullable = false, length = 64)
    private String facilityId;

    /** 工单标题 */
    @Column(nullable = false, length = 256)
    private String title;

    /** 工单描述 */
    @Column(columnDefinition = "text")
    private String description;

    /** 优先级（LOW/MEDIUM/HIGH/URGENT） */
    @Column(length = 16)
    private String priority;

    /** 状态（PENDING/IN_PROGRESS/COMPLETED/CANCELLED） */
    @Column(length = 16)
    private String status;

    /** 责任部门 */
    @Column(name = "assigned_dept", length = 128)
    private String assignedDept;

    /** 责任人 */
    @Column(name = "assigned_person", length = 64)
    private String assignedPerson;

    /** 处理进度（0-100） */
    @Column(nullable = false)
    private Integer progress;

    /** 截止日期 */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** 完成时间 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 创建时间（数据库默认值生成） */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间（数据库自动维护） */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

}