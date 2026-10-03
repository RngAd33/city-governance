package com.cityplatform.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 地理位置信息
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeoLocation {
    private Double lat;
    private Double lng;
    private String address;

    public GeoLocation() {}

    public GeoLocation(Double lat, Double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public GeoLocation(Double lat, Double lng, String address) {
        this.lat = lat;
        this.lng = lng;
        this.address = address;
    }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public String toString() {
        return "GeoLocation{lat=" + lat + ", lng=" + lng + ", address='" + address + "'}";
    }
}
