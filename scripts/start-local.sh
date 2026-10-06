#!/bin/bash
# 本地开发环境启动脚本

echo "启动城市治理平台本地开发环境..."

# 启动基础设施
echo "启动 RabbitMQ, MySQL, Redis..."
docker-compose up -d rabbitmq mysql redis

# 等待服务就绪
echo "等待服务就绪..."
sleep 10

# 启动微服务
echo "启动微服务..."
cd city-collector && mvn spring-boot:run &
cd city-traffic && mvn spring-boot:run &
cd city-energy && mvn spring-boot:run &
cd city-environment && mvn spring-boot:run &
cd city-water && mvn spring-boot:run &
cd city-disaster && mvn spring-boot:run &
cd city-govai && mvn spring-boot:run &

echo "所有服务已启动"
echo "RabbitMQ管理界面: http://localhost:15672"
echo "API服务: http://localhost:8081-8088"
