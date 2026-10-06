package com.cityplatform.collector.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 城市事件记录实体
 * 用于持久化采集到的城市事件数据
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "event_record")
public class EventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", length = 64, nullable = false)
    private String eventId;

    @Column(name = "domain", length = 32, nullable = false)
    private String domain;

    @Column(name = "city", length = 64, nullable = false)
    private String city;

    @Column(name = "metric", length = 64, nullable = false)
    private String metric;

    @Column(name = "value", nullable = false)
    private Double value;

    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "source", length = 32, nullable = false)
    private String source;

    @Column(name = "ts", nullable = false)
    private OffsetDateTime ts;

    /** 业务便利构造器：不含自增主键 id */
    public EventRecord(String eventId, String domain, String city, String metric,
                       Double value, String unit, String source, OffsetDateTime ts) {
        this.eventId = eventId;
        this.domain = domain;
        this.city = city;
        this.metric = metric;
        this.value = value;
        this.unit = unit;
        this.source = source;
        this.ts = ts;
    }
}
