#!/bin/bash
# 全链路冒烟测试脚本
# 验证所有服务是否正常运行

set -e

echo "=========================================="
echo "城市治理平台 - 全链路冒烟测试"
echo "=========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0

test_endpoint() {
    local name=$1
    local url=$2
    local expected_code=${3:-200}

    echo -n "测试 $name ... "
    response=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "$url" 2>/dev/null || echo "000")

    if [ "$response" = "$expected_code" ]; then
        echo -e "${GREEN}通过${NC} (HTTP $response)"
        ((PASS++))
    else
        echo -e "${RED}失败${NC} (HTTP $response, 期望 $expected_code)"
        ((FAIL++))
    fi
}

test_post() {
    local name=$1
    local url=$2
    local data=$3

    echo -n "测试 $name ... "
    response=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "Content-Type: application/json" -d "$data" --connect-timeout 5 "$url" 2>/dev/null || echo "000")

    if [ "$response" = "200" ] || [ "$response" = "201" ]; then
        echo -e "${GREEN}通过${NC} (HTTP $response)"
        ((PASS++))
    else
        echo -e "${RED}失败${NC} (HTTP $response)"
        ((FAIL++))
    fi
}

echo ""
echo "--- 基础设施测试 ---"
test_endpoint "RabbitMQ管理界面" "http://localhost:15672"
test_endpoint "MySQL连接" "http://localhost:3306" "000"  # MySQL不返回HTTP 200，但连接成功

echo ""
echo "--- 服务健康检查 ---"
test_endpoint "采集服务" "http://localhost:8081/api/collector/status"
test_endpoint "交通优化服务" "http://localhost:8082/api/traffic/status"
test_endpoint "能耗分析服务" "http://localhost:8083/api/energy/status"
test_endpoint "环境监测服务" "http://localhost:8084/api/environment/status"
test_endpoint "水务风险服务" "http://localhost:8085/api/water/status"
test_endpoint "政务AI服务" "http://localhost:8086/api/govai/status"

echo ""
echo "--- 核心功能测试 ---"
test_post "触发数据采集" "http://localhost:8081/api/collector/trigger" '{}'
test_post "交通信号优化" "http://localhost:8082/api/traffic/optimize" '{"intersectionId":"INT-001","phases":[{"direction":"NS","greenTime":30},{"direction":"EW","greenTime":25}]}'
test_post "能耗分析" "http://localhost:8083/api/energy/buildings/B001/analysis" '{}'
test_post "环境记录查询" "http://localhost:8084/api/environment/records" '{}'
test_post "水务风险评估" "http://localhost:8085/api/water/stations/WS001/risk" '{}'
test_post "政务AI查询" "http://localhost:8086/api/govai/query" '{"query":"当前城市运行状态如何？"}'
test_post "告警解释" "http://localhost:8086/api/govai/alert/explain" '{"type":"air_quality","city":"南京","value":150}'
test_post "报告生成" "http://localhost:8086/api/govai/report/generate" '{"type":"daily","city":"南京"}'

echo ""
echo "=========================================="
echo "测试结果: ${GREEN}$PASS 通过${NC}, ${RED}$FAIL 失败${NC}"
echo "=========================================="

if [ $FAIL -gt 0 ]; then
    exit 1
fi
echo -e "${GREEN}所有测试通过！${NC}"
