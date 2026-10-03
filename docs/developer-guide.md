# 开发指南

## 1. 开发环境搭建

### 1.1 前置要求
- JDK 21+
- Maven 3.8+
- Docker 20.10+
- Docker Compose 2.0+
- IDE (IntelliJ IDEA / Eclipse / VS Code)

### 1.2 项目导入
```bash
git clone <repository>
cd city-governance-platform
mvn clean install
```

### 1.3 本地运行
```bash
# 启动基础设施
docker-compose up -d rabbitmq mysql redis

# 启动服务（按顺序）
cd city-collector && mvn spring-boot:run
cd city-traffic && mvn spring-boot:run
cd city-energy && mvn spring-boot:run
cd city-environment && mvn spring-boot:run
cd city-water && mvn spring-boot:run
cd city-govai && mvn spring-boot:run
```

## 2. 代码规范

### 2.1 命名规范
- 类名：大驼峰（PascalCase）
- 方法名：小驼峰（camelCase）
- 常量：全大写下划线分隔
- 包名：全小写

### 2.2 注释规范
- 类注释：说明类的用途
- 方法注释：说明方法功能、参数、返回值
- 关键逻辑：行内注释

### 2.3 异常处理
- 使用try-catch捕获异常
- 记录详细日志
- 返回友好错误信息

## 3. 测试规范

### 3.1 单元测试
- 使用JUnit 5
- 测试覆盖率 > 70%
- Mock外部依赖

### 3.2 集成测试
- 使用@Testcontainers
- 测试事件总线通信
- 测试端到端流程

## 4. 部署规范

### 4.1 构建
```bash
mvn clean package -DskipTests
```

### 4.2 Docker镜像
```bash
docker build -t city-collector ./city-collector
docker build -t city-traffic ./city-traffic
# ...
```

### 4.3 部署
```bash
docker-compose up -d
```

## 5. 常见问题

### 5.1 端口冲突
修改application.yml中的server.port

### 5.2 数据库连接失败
检查MySQL服务状态和连接配置

### 5.3 RabbitMQ连接失败
检查RabbitMQ服务状态和认证信息
