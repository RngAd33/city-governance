package com.cityplatform.common.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ城市事件总线配置
 * 主题交换机：city.event.topic
 * 10个队列对应10类城市事件
 */
@Configuration
public class EventBusConfig {

    public static final String TOPIC_EXCHANGE = "city.event.topic";

    // 队列名
    public static final String ENV_WEATHER_QUEUE = "env.weather.queue";
    public static final String ENV_AIRQUALITY_QUEUE = "env.airquality.queue";
    public static final String TRAFFIC_FLOW_QUEUE = "traffic.flow.queue";
    public static final String TRAFFIC_SIGNAL_QUEUE = "traffic.signal.queue";
    public static final String ENERGY_BUILDING_QUEUE = "energy.building.queue";
    public static final String ENERGY_GRID_QUEUE = "energy.grid.queue";
    public static final String WATER_MONITOR_QUEUE = "water.monitor.queue";
    public static final String DISASTER_SIM_QUEUE = "disaster.sim.queue";
    public static final String INFRA_MONITOR_QUEUE = "infra.monitor.queue";
    public static final String GOVAI_DECISION_QUEUE = "govai.decision.queue";

    // Routing Keys
    public static final String RK_ENV_WEATHER = "env.weather.city";
    public static final String RK_ENV_AIRQUALITY = "env.airquality.city";
    public static final String RK_TRAFFIC_FLOW = "traffic.flow.segment";
    public static final String RK_TRAFFIC_SIGNAL = "traffic.signal.optimization";
    public static final String RK_ENERGY_BUILDING = "energy.building.consumption";
    public static final String RK_ENERGY_GRID = "energy.grid.indicator";
    public static final String RK_WATER_MONITOR = "water.level.station";
    public static final String RK_DISASTER_SIM = "disaster.flood.scenario";
    public static final String RK_INFRA_STATUS = "infra.facility.status";
    public static final String RK_GOVAI_DECISION = "govai.decision.suggest";

    @Bean
    public TopicExchange cityEventExchange() {
        return ExchangeBuilder.topicExchange(TOPIC_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue envWeatherQueue() {
        return QueueBuilder.durable(ENV_WEATHER_QUEUE).build();
    }

    @Bean
    public Queue envAirQualityQueue() {
        return QueueBuilder.durable(ENV_AIRQUALITY_QUEUE).build();
    }

    @Bean
    public Queue trafficFlowQueue() {
        return QueueBuilder.durable(TRAFFIC_FLOW_QUEUE).build();
    }

    @Bean
    public Queue trafficSignalQueue() {
        return QueueBuilder.durable(TRAFFIC_SIGNAL_QUEUE).build();
    }

    @Bean
    public Queue energyBuildingQueue() {
        return QueueBuilder.durable(ENERGY_BUILDING_QUEUE).build();
    }

    @Bean
    public Queue energyGridQueue() {
        return QueueBuilder.durable(ENERGY_GRID_QUEUE).build();
    }

    @Bean
    public Queue waterMonitorQueue() {
        return QueueBuilder.durable(WATER_MONITOR_QUEUE).build();
    }

    @Bean
    public Queue disasterSimQueue() {
        return QueueBuilder.durable(DISASTER_SIM_QUEUE).build();
    }

    @Bean
    public Queue infraMonitorQueue() {
        return QueueBuilder.durable(INFRA_MONITOR_QUEUE).build();
    }

    @Bean
    public Queue govaiDecisionQueue() {
        return QueueBuilder.durable(GOVAI_DECISION_QUEUE).build();
    }

    // Binding
    @Bean
    public Binding envWeatherBinding() {
        return BindingBuilder.bind(envWeatherQueue()).to(cityEventExchange()).with(RK_ENV_WEATHER);
    }

    @Bean
    public Binding envAirQualityBinding() {
        return BindingBuilder.bind(envAirQualityQueue()).to(cityEventExchange()).with(RK_ENV_AIRQUALITY);
    }

    @Bean
    public Binding trafficFlowBinding() {
        return BindingBuilder.bind(trafficFlowQueue()).to(cityEventExchange()).with(RK_TRAFFIC_FLOW);
    }

    @Bean
    public Binding trafficSignalBinding() {
        return BindingBuilder.bind(trafficSignalQueue()).to(cityEventExchange()).with(RK_TRAFFIC_SIGNAL);
    }

    @Bean
    public Binding energyBuildingBinding() {
        return BindingBuilder.bind(energyBuildingQueue()).to(cityEventExchange()).with(RK_ENERGY_BUILDING);
    }

    @Bean
    public Binding energyGridBinding() {
        return BindingBuilder.bind(energyGridQueue()).to(cityEventExchange()).with(RK_ENERGY_GRID);
    }

    @Bean
    public Binding waterMonitorBinding() {
        return BindingBuilder.bind(waterMonitorQueue()).to(cityEventExchange()).with(RK_WATER_MONITOR);
    }

    @Bean
    public Binding disasterSimBinding() {
        return BindingBuilder.bind(disasterSimQueue()).to(cityEventExchange()).with(RK_DISASTER_SIM);
    }

    @Bean
    public Binding infraMonitorBinding() {
        return BindingBuilder.bind(infraMonitorQueue()).to(cityEventExchange()).with(RK_INFRA_STATUS);
    }

    @Bean
    public Binding govaiDecisionBinding() {
        return BindingBuilder.bind(govaiDecisionQueue()).to(cityEventExchange()).with(RK_GOVAI_DECISION);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setExchange(TOPIC_EXCHANGE);
        return template;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
