# HomeGuard 家庭管理系统

面向家庭场景的资产与物品管理平台，提供家庭物资库存、衣服收纳、家庭财富及系统权限等能力。前端品牌名为 **HomeGuard**，后端服务名为 `family-manage`。

## 功能概览

| 模块 | 说明 |
|------|------|
| **全部资产** | 管理家庭日常物资（食品、调味品等），支持分类、位置、数量、低库存预警、到期提醒 |
| **衣服管理** | 衣服明细、衣服类型、存放位置；支持按区域编码维护衣柜/箱子位置 |
| **财富管理** | 资产总览（按类型汇总金额）、资产类型维护（现金、微信、支付宝等） |
| **系统管理** | 用户、角色、菜单权限；基于 RBAC 控制各功能访问 |

登录后侧边栏仅展示当前用户角色已授权的菜单。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 8、Spring Boot 2.7.18、MyBatis-Plus 3.5、Druid、动态数据源 |
| 前端 | Vue 2.7、Vite 4、Axios |
| 数据库 | MySQL 5.7+ / 8.x |
| 部署 | Docker（可选）、Nginx 反向代理 |

## 项目结构

```
family-manage/
├── family-manage-backend/     # Spring Boot 后端
│   ├── src/main/java/         # 业务代码
│   └── src/main/resources/
│       ├── application.yml    # 应用与数据源配置
│       └── db/cc_test_schema.sql  # 数据库初始化脚本
├── family-mange-frontend/     # Vue 前端（目录名为 mange，非 manage）
│   ├── src/
│   │   ├── api/               # 接口封装
│   │   ├── views/             # 页面组件
│   │   └── components/        # 公共组件（如可拖拽列宽表格）
│   └── vite.config.js         # 开发代理 /api -> 后端
├── docker-build/              # Docker 镜像构建脚本与配置
├── 财富管理.md                 # 财富管理需求说明
├── 衣服管理模块需求.md          # 衣服管理需求说明
└── README.md
```

## 环境要求

- **JDK** 1.8+
- **Maven** 3.6+
- **Node.js** 16+（建议 18+）
- **MySQL** 可访问的实例

## 快速开始

### 1. 初始化数据库

在 MySQL 中执行初始化脚本：

```bash
mysql -u root -p < family-manage-backend/src/main/resources/db/cc_test_schema.sql
```

脚本会创建数据库 `cc_test`，并初始化表结构、示例数据、默认菜单与管理员账号。

> 若应用首次启动且库表不存在，后端 `DatabaseInitializer` 也会尝试自动建表并写入种子数据。

### 2. 配置后端数据源

编辑 `family-manage-backend/src/main/resources/application.yml`，修改主数据源 `master` 的连接信息：

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          url: jdbc:mysql://<主机>:3306/cc_test?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Hongkong
          username: root
          password: <你的密码>
```

配置项说明：

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `server.port` | `12300` | 后端 HTTP 端口 |
| 主库 `master` | `cc_test` | 业务主库，存放全部业务表 |
| 从库 `old_km` | 可选 | 历史库连接，未使用时可保留或删除 |

Druid 监控（可选）：启用后可通过 `/druid` 访问，默认账号 `admin` / `admin`（见 `application.yml`）。

### 3. 启动后端

```bash
cd family-manage-backend
mvn spring-boot:run
```

或打包后运行：

```bash
mvn -DskipTests package
java -jar target/family-manage-backend-1.0.0.jar
```

服务地址：`http://localhost:12300`

### 4. 启动前端

```bash
cd family-mange-frontend
npm install
npm run dev
```

开发服务器：`http://localhost:5173`  
Vite 已将 `/api` 代理到 `http://localhost:12300`。

生产构建：

```bash
npm run build
npm run preview   # 本地预览 dist，端口 5174
```

### 5. 登录系统

| 项目 | 值 |
|------|-----|
| 默认账号 | `admin` |
| 默认密码 | `admin123` |

密码存储为 `SHA-256`，盐值为 `family-manage:<明文密码>`。生产环境请及时修改默认密码。

## Docker 部署

在 Windows 下可使用 `docker-build/build-all-images.bat` 一键构建前后端镜像：

```bat
cd docker-build
build-all-images.bat [tag] [backend-image-name] [frontend-image-name]
```

示例（使用默认标签 `latest`）：

```bat
build-all-images.bat
```

脚本会依次：Maven 打包后端 → `npm run build` 前端 → 构建 Docker 镜像。

运行示例：

```bash
docker network create family-manage-net

docker run -d --name family-manage-backend \
  --network family-manage-net \
  -p 12300:12300 \
  family-manage-backend:latest

docker run -d --name family-manage-frontend \
  --network family-manage-net \
  -p 8080:80 \
  family-manage-frontend:latest
```

访问：`http://localhost:8080`（Nginx 将 `/api` 转发至容器 `family-manage-backend:12300`）。

> 容器内后端仍使用镜像构建时的 `application.yml`。若数据库地址与构建环境不同，需通过挂载外部配置或环境变量等方式覆盖数据源配置。

## 权限与角色

系统采用 **用户 → 角色 → 菜单权限** 模型：

- 菜单以 `code` 标识（如 `asset:view`、`clothes:item`），与接口 `@RequirePermission` 及前端视图映射一致。
- 预置角色：
  - **admin（家庭管理员）**：全部菜单
  - **member（家庭成员）**：全部资产、衣服管理、财富管理（不含系统管理）

接口鉴权：除 `POST /api/auth/login` 外，请求需携带 Header：

```
Authorization: Bearer <token>
```

或：

```
X-Auth-Token: <token>
```

## API 概览

统一响应格式：

```json
{
  "code": 200,
  "message": "ok",
  "data": { }
}
```

业务失败时 `code` 为 401/403 等，`message` 为错误说明。

| 前缀 | 功能 |
|------|------|
| `/api/auth` | 登录、当前用户、登出 |
| `/api/assets` | 家庭资产 CRUD、汇总、库存调整 |
| `/api/clothes` | 衣服明细 / 类型 / 位置 |
| `/api/wealth` | 财富资产与资产类型 |
| `/api/system` | 用户、角色、菜单管理 |

主要接口示例：

```
POST   /api/auth/login
GET    /api/auth/me
POST   /api/auth/logout

GET    /api/assets
GET    /api/assets/summary
POST   /api/assets
PUT    /api/assets/{id}
DELETE /api/assets/{id}

GET    /api/clothes/items
GET    /api/clothes/types
GET    /api/clothes/locations
GET    /api/clothes/options

GET    /api/wealth/assets
GET    /api/wealth/types
GET    /api/wealth/options

GET    /api/system/users
GET    /api/system/roles
GET    /api/system/menus
```

## 数据库表说明

| 表名 | 用途 |
|------|------|
| `family_asset_item` | 家庭日常资产 |
| `clothes_type` / `clothes_location` / `clothes_item` | 衣服类型、位置、明细 |
| `wealth_type` / `wealth_asset` | 财富类型与资产明细 |
| `sys_user` / `sys_role` / `sys_menu` | 用户、角色、菜单 |
| `sys_user_role` / `sys_role_menu` | 用户角色、角色菜单关联 |
| `sys_auth_token` | 登录令牌 |

## 前端说明

- 环境变量：`family-mange-frontend/.env` 中 `VITE_API_BASE_URL=/api`
- 列表页使用 `ResizableTable` 组件，表头列宽支持拖拽调整
- 视图与菜单 `code` 映射见 `App.vue` 中 `viewMap`

## 开发说明

### 新增菜单与页面

1. 在 `sys_menu` 中增加菜单记录（或通过「菜单权限」页面维护）
2. 为角色分配对应菜单
3. 后端 Controller 方法添加 `@RequirePermission("菜单code")`
4. 前端在 `App.vue` 的 `viewMap` 中注册组件

### 本地调试建议

1. 先确认 MySQL 与 `application.yml` 中数据源一致
2. 启动后端，访问 `http://localhost:12300/api/auth/login` 验证
3. 再启动前端开发服务器

## 常见问题

**Q: 前端请求 404 或跨域？**  
开发模式请使用 `npm run dev`，由 Vite 代理 `/api`；勿直接以 `file://` 打开页面。

**Q: 登录提示账号或密码错误？**  
确认已执行 SQL 初始化，或删除 `sys_user` 后重启后端让 `DatabaseInitializer` 重建管理员。

**Q: 看不到某些菜单？**  
检查当前用户角色是否已分配对应菜单权限。

**Q: `family-mange-frontend` 拼写？**  
历史目录命名如此，文档与脚本均按实际路径 `family-mange-frontend` 使用。

## 相关文档

- [财富管理需求](./财富管理.md)
- [衣服管理模块需求](./衣服管理模块需求.md)

## 许可证

本项目为私有/内部使用项目，未在仓库中声明开源许可证。如需对外发布，请自行补充许可说明。
