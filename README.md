# 金酒精准营销系统 (jinjiuDRTM)

基于地理大数据的精准营销系统 (Geo-BI)，面向酒水/快消行业。

## 项目概述

### 产品定位
面向酒水/快消行业，基于LBS与多维数据融合的"地图化商业智能"决策指挥平台。

### 核心功能
1. **前端市场洞察** - 网点挖掘、渠道活跃度、餐饮街区、订单流向
2. **用户洞察与推广** - 产品推广选区、ROI预测
3. **管理赋能** - 薄弱区域洞察、营销人员效能评估

## 技术架构

### 后端
- **框架**: Spring Cloud 2023.0.0 + Spring Boot 3.2.0
- **服务**: Spring Cloud Gateway (网关) + 4个业务服务
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **地理引擎**: H3 (Uber)
- **认证**: Sa-Token

### 前端
- **框架**: Vue 3 + Composition API
- **UI**: Element Plus
- **状态**: Pinia
- **地图**: ECharts (GIS可视化)
- **构建**: Vite 5

### 微服务模块
| 服务 | 端口 | 说明 |
|------|------|------|
| gateway | 8080 | API网关 |
| system | 8081 | 系统管理(用户/角色/权限) |
| map | 8082 | 地图服务(H3网格/GIS) |
| market | 8083 | 市场洞察(网点/渠道/流向) |
| analysis | 8084 | 数据分析(薄弱洞察/效能评估) |

## 项目结构

```
jinjiuDRTM/
├── backend/                    # 后端 Spring Cloud
│   ├── jinjiuDRTM-common/     # 公共模块
│   ├── jinjiuDRTM-gateway/    # 网关服务
│   └── jinjiuDRTM-modules/    # 业务模块
│       ├── jinjiuDRTM-system/ # 系统管理
│       ├── jinjiuDRTM-map/    # 地图服务
│       ├── jinjiuDRTM-market/ # 市场洞察
│       └── jinjiuDRTM-analysis/ # 数据分析
├── frontend/                   # 前端 Vue 3
│   └── src/
│       ├── api/               # API调用
│       ├── components/        # 组件
│       ├── views/            # 页面
│       ├── router/           # 路由
│       ├── store/            # 状态管理
│       └── layout/           # 布局
└── docs/                      # 文档
```

## 快速开始

### 前置条件
- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+

### 后端启动

```bash
cd backend

# 初始化数据库
mysql -u root -p < sql/init.sql

# 编译项目
mvn clean install

# 启动网关(端口8080)
cd jinjiuDRTM-gateway && mvn spring-boot:run

# 启动业务服务(并行窗口)
cd ../jinjiuDRTM-modules/jinjiuDRTM-system && mvn spring-boot:run
cd ../jinjiuDRTM-map && mvn spring-boot:run
cd ../jinjiuDRTM-market && mvn spring-boot:run
cd ../jinjiuDRTM-analysis && mvn spring-boot:run
```

### 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev

# 生产构建
npm run build
```

访问 http://localhost:3000

## Phase 1 MVP 功能

- [x] 前端分布热力图 (`/market/heatmap`)
- [x] 空白网点挖掘 (`/market/insight`)
- [x] 订单流向弧线图 (`/market/flow`)

## Phase 2 & 3 规划

- [ ] 渠道活跃度识别
- [ ] 核心氛围街区打造
- [ ] 产品推广选区模型
- [ ] ROI预测沙盘
- [ ] 薄弱区域红绿灯预警
- [ ] 营销人员效能评估

## 默认账号

- 用户名: admin
- 密码: admin123

## License

Private
