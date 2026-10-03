package com.cityplatform.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 城市统一事件模型
 * 所有微服务之间通过此事件格式通信
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CityEvent {

    private String eventId;
    private EventDomain domain;
    private String city;
    private GeoLocation location;
    private String metric;
    private Double value;
    private String unit;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime ts;

    private EventSource source;
    private String rawData;

    public CityEvent() {}

    public CityEvent(EventDomain domain, String city, String metric, Double value, String unit, EventSource source) {
        this.domain = domain;
        this.city = city;
        this.metric = metric;
        this.value = value;
        this.unit = unit;
        this.source = source;
        this.ts = OffsetDateTime.now();
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public EventDomain getDomain() { return domain; }
    public void setDomain(EventDomain domain) { this.domain = domain; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public GeoLocation getLocation() { return location; }
    public void setLocation(GeoLocation location) { this.location = location; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public OffsetDateTime getTs() { return ts; }
    public void setTs(OffsetDateTime ts) { this.ts = ts; }

    public EventSource getSource() { return source; }
    public void setSource(EventSource source) { this.source = source; }

    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CityEvent cityEvent = (CityEvent) o;
        return Objects.equals(eventId, cityEvent.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "CityEvent{" +
                "eventId='" + eventId + '\'' +
                ", domain=" + domain +
                ", city='" + city + '\'' +
                ", metric='" + metric + '\'' +
                ", value=" + value +
                ", unit='" + unit + '\'' +
                ", ts=" + ts +
                ", source=" + source +
                '}';
    }
}
