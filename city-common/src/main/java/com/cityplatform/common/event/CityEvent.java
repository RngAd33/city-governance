package com.cityplatform.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 城市统一事件模型
 * 所有微服务之间通过此事件格式通信
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "eventId")
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

    /** 业务便利构造器：创建带当前时间戳的事件（eventId/location/rawData 由调用方补充） */
    public CityEvent(EventDomain domain, String city, String metric, Double value, String unit, EventSource source) {
        this.domain = domain;
        this.city = city;
        this.metric = metric;
        this.value = value;
        this.unit = unit;
        this.source = source;
        this.ts = OffsetDateTime.now();
    }
}
