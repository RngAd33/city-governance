package com.cityplatform.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 地理位置信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeoLocation {
    private Double lat;
    private Double lng;
    private String address;

    /** 业务便利构造器：仅经纬度场景（地址由调用方补充） */
    public GeoLocation(Double lat, Double lng) {
        this.lat = lat;
        this.lng = lng;
    }
}
