package com.cityplatform.traffic.constant;

public interface TrafficSimulatorConstants {

    // 饱和流量(辆/小时/车道)，默认值1800
    double DEFAULT_SATURATION_FLOW = 1800.0;
    // 过饱和排队基座：M/D/1 在 rho=0.999 处的稳态队列长度，使过饱和段与稳态段平滑衔接
    double OVERFLOW_QUEUE_BASE = 499.0;
    // 自由流速度(km/h)
    double FREE_FLOW_SPEED = 50.0;
    // 车辆平均长度(m)
    double VEHICLE_LENGTH = 5.0;
    // 最小车头间距(m)
    double MIN_HEADWAY = 2.0;

}