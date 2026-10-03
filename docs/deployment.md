# 部署文档

## 1. 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- Java 21+
- Maven 3.8+
- 4GB+ 可用内存

## 2. Docker Compose 部署

### 2.1 编译

```bash
cd city-governance-platform
mvn clean package -DskipTests
```

### 2.2 启动

```bash
docker-compose up -d
```

### 2.3 验证

```bash
# 检查服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 运行冒烟测试
./scripts/smoke-test.sh
```

### 2.4 停止

```bash
docker-compose down
```

## 3. 端口映射

| 服务 | 端口 | 说明 |
|------|------|------|
| RabbitMQ | 5672 | AMQP |
| RabbitMQ管理 | 15672 | Web管理界面 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| collector | 8081 | 数据采集 |
| traffic | 8082 | 交通优化 |
| energy | 8083 | 能耗分析 |
| environment | 8084 | 环境监测 |
| water | 8085 | 水务风险 |
| govai | 8086 | 政务AI |

## 4. 默认凭据

- RabbitMQ: admin / admin123
- MySQL: cityuser / citypass (root: root123)
