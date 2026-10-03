#!/bin/bash
# 本地开发环境停止脚本

echo "停止城市治理平台本地开发环境..."

# 停止微服务
echo "停止微服务..."
pkill -f "spring-boot:run"

# 停止基础设施
echo "停止基础设施..."
docker-compose down

echo "所有服务已停止"
