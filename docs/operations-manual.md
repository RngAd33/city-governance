# 运维手册

## 1. 日常运维

### 1.1 服务状态检查
```bash
# 检查所有服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f [service-name]

# 检查RabbitMQ
curl -u admin:admin123 http://localhost:15672/api/overview
```

### 1.2 数据备份
```bash
# 备份MySQL数据
docker exec city-mysql mysqldump -u root -proot123 city_platform > backup.sql

# 恢复数据
docker exec -i city-mysql mysql -u root -proot123 city_platform < backup.sql
```

### 1.3 性能监控
```bash
# 查看容器资源使用
docker stats

# 查看JVM状态
jstat -gcutil <pid> 1000
```

## 2. 故障处理

### 2.1 服务启动失败
```bash
# 查看启动日志
docker-compose logs [service-name]

# 检查端口占用
netstat -tlnp | grep 8080

# 重启服务
docker-compose restart [service-name]
```

### 2.2 RabbitMQ连接失败
```bash
# 检查RabbitMQ状态
docker-compose logs rabbitmq

# 验证连接
telnet localhost 5672
```

### 2.3 数据库连接失败
```bash
# 检查MySQL状态
docker-compose logs mysql

# 验证连接
mysql -u cityuser -pcitypass -h localhost city_platform
```

## 3. 扩展指南

### 3.1 添加新服务
1. 创建新模块目录
2. 添加POM文件
3. 实现服务代码
4. 更新docker-compose.yml

### 3.2 添加新事件类型
1. 在EventDomain中添加枚举
2. 在EventBusConfig中添加队列和路由键
3. 实现消费者

### 3.3 添加新告警规则
1. 通过API创建规则
2. 规则自动生效
3. 告警记录入库
