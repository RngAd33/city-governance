#!/bin/bash
# 微服务启动脚本

echo "启动城市治理平台微服务..."

# 等待基础设施就绪
echo "等待 RabbitMQ 就绪..."
until docker exec city-governance-platform-rabbitmq-1 rabbitmq-diagnostics -q ping 2>/dev/null; do
    sleep 2
done
echo "RabbitMQ 就绪"

echo "等待 MySQL 就绪..."
until docker exec city-governance-platform-mysql-1 mysqladmin ping -h localhost -uroot -proot123 2>/dev/null; do
    sleep 2
done
echo "MySQL 就绪"

# 启动微服务
echo "启动 city-collector..."
docker-compose up -d collector
sleep 5

echo "启动 city-traffic..."
docker-compose up -d traffic
sleep 5

echo "启动 city-energy..."
docker-compose up -d energy
sleep 5

echo "启动 city-environment..."
docker-compose up -d environment
sleep 5

echo "启动 city-water..."
docker-compose up -d water
sleep 5

echo "启动 city-govai..."
docker-compose up -d govai
sleep 5

echo "所有服务已启动"
docker-compose ps
