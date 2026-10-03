package com.cityplatform.collector.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 城市事件记录实体
 * 用于持久化采集到的城市事件数据
 */
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

    public EventRecord() {}

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public OffsetDateTime getTs() { return ts; }
    public void setTs(OffsetDateTime ts) { this.ts = ts; }

    @Override
    public String toString() {
        return "EventRecord{" +
                "id=" + id +
                ", eventId='" + eventId + '\'' +
                ", domain='" + domain + '\'' +
                ", city='" + city + '\'' +
                ", metric='" + metric + '\'' +
                ", value=" + value +
                ", unit='" + unit + '\'' +
                ", source='" + source + '\'' +
                ", ts=" + ts +
                '}';
    }
}
