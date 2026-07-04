# 校园二手交易平台 — 开发设计文档

## 1. 项目概述

### 1.1 项目名称
**CampusTrade** — 校园二手交易平台

### 1.2 一句话描述
面向在校大学生的二手物品发布、浏览、搜索、收藏与在线留言沟通平台。

### 1.3 核心功能
| 模块 | 功能点 |
|---|---|
| 用户系统 | 注册、登录、JWT 认证、个人信息修改 |
| 商品管理 | 发布商品、编辑、下架/标记已售、列表分页 |
| 浏览筛选 | 按分类筛选、按价格排序、按成色筛选、关键词搜索 |
| 互动功能 | 商品留言（问价/沟通）、收藏商品 |
| 个人中心 | 我的发布、我的收藏、个人信息 |

### 1.4 技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 后端框架 | Spring Boot | 3.x |
| ORM | MyBatis-Plus | 3.5+ |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x（可选，面试加分项） |
| 认证 | JWT（jjwt 库） | 0.12+ |
| 前端框架 | Vue 3 + Composition API | 3.4+ |
| UI 组件库 | Element Plus | 2.x |
| 构建工具 | Vite | 5.x |
| 部署 | 后端 Railway/Render，前端 Vercel | - |

---

## 2. 数据库设计

### 2.1 ER 图（文字版）

```
User (1) ----< (N) Product   用户发布多个商品
User (1) ----< (N) Message   用户留多条言
User (1) ----< (N) Bookmark  用户收藏多个商品
Product (1) ----< (N) Message   一个商品有多条留言
Product (1) ----< (N) Bookmark  一个商品被多人收藏
```

### 2.2 建表 SQL

```sql
-- 用户表
CREATE TABLE `user` (
  `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username`   VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
  `password`   VARCHAR(255) NOT NULL COMMENT 'BCrypt加密后的密码',
  `email`      VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
  `phone`      VARCHAR(20)  DEFAULT '' COMMENT '手机号',
  `avatar_url` VARCHAR(500) DEFAULT '' COMMENT '头像URL',
  `school`     VARCHAR(100) DEFAULT '' COMMENT '学校',
  `bio`        VARCHAR(255) DEFAULT '' COMMENT '个人简介',
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品表
CREATE TABLE `product` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title`       VARCHAR(100)  NOT NULL COMMENT '商品标题',
  `description` TEXT          NOT NULL COMMENT '商品描述',
  `price`       DECIMAL(10,2) NOT NULL COMMENT '价格',
  `condition`   VARCHAR(20)   NOT NULL DEFAULT 'GOOD' COMMENT '成色: NEW/LIKE_NEW/GOOD/FAIR',
  `category`    VARCHAR(50)   NOT NULL COMMENT '分类: 电子产品/书籍教材/生活用品/运动器材/服饰鞋包/数码配件/其他',
  `image_url`   VARCHAR(500)  DEFAULT '' COMMENT '商品图片URL',
  `status`      VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态: AVAILABLE(在售)/SOLD(已售)/REMOVED(已下架)',
  `view_count`  INT           DEFAULT 0 COMMENT '浏览量',
  `user_id`     BIGINT        NOT NULL COMMENT '发布者ID',
  `created_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_category` (`category`),
  INDEX `idx_status` (`status`),
  INDEX `idx_price` (`price`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_created_at` (`created_at`),
  FULLTEXT INDEX `ft_title_desc` (`title`, `description`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 留言表
CREATE TABLE `message` (
  `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
  `content`    TEXT     NOT NULL COMMENT '留言内容',
  `user_id`    BIGINT   NOT NULL COMMENT '留言者ID',
  `product_id` BIGINT   NOT NULL COMMENT '商品ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_product_id` (`product_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留言表';

-- 收藏表
CREATE TABLE `bookmark` (
  `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id`    BIGINT   NOT NULL COMMENT '收藏者ID',
  `product_id` BIGINT   NOT NULL COMMENT '商品ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';
```

---

## 3. API 接口设计

### 3.1 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

HTTP 状态码规范：
| code | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

### 3.2 接口清单

#### 用户模块

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| POST | `/api/user/register` | 注册 | 否 |
| POST | `/api/user/login` | 登录 | 否 |
| GET | `/api/user/me` | 获取当前用户信息 | 是 |
| PUT | `/api/user/me` | 修改个人信息 | 是 |
| GET | `/api/user/{id}` | 查看用户公开信息 | 否 |
| GET | `/api/user/{id}/products` | 查看某用户发布的商品 | 否 |

#### 商品模块

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| GET | `/api/product` | 商品列表（分页+筛选+排序） | 否 |
| GET | `/api/product/{id}` | 商品详情 | 否 |
| POST | `/api/product` | 发布商品 | 是 |
| PUT | `/api/product/{id}` | 编辑商品 | 是（仅作者） |
| PATCH | `/api/product/{id}/status` | 修改商品状态（已售/下架） | 是（仅作者） |
| DELETE | `/api/product/{id}` | 删除商品 | 是（仅作者） |

#### 留言模块

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| GET | `/api/product/{id}/message` | 获取商品留言列表 | 否 |
| POST | `/api/product/{id}/message` | 发送留言 | 是 |
| DELETE | `/api/message/{id}` | 删除留言 | 是（留言作者或商品作者） |

#### 收藏模块

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| POST | `/api/product/{id}/bookmark` | 收藏/取消收藏（toggle） | 是 |
| GET | `/api/user/bookmarks` | 我的收藏列表 | 是 |

#### 搜索模块

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| GET | `/api/product/search` | 关键词搜索商品 | 否 |

### 3.3 关键接口详情

#### `GET /api/product` — 商品列表

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 12 |
| category | String | 否 | 分类筛选 |
| condition | String | 否 | 成色筛选 |
| sort | String | 否 | 排序：`newest`(默认) / `cheapest` / `expensive` |
| keyword | String | 否 | 搜索关键词 |

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "title": "八成新 MacBook Air M3",
        "price": 4500.00,
        "condition": "GOOD",
        "category": "电子产品",
        "imageUrl": "https://...",
        "status": "AVAILABLE",
        "bookmarkCount": 12,
        "messageCount": 5,
        "author": {
          "id": 1,
          "username": "张三",
          "avatarUrl": "https://..."
        },
        "createdAt": "2026-07-03T10:30:00"
      }
    ],
    "total": 56,
    "page": 1,
    "size": 12,
    "pages": 5
  }
}
```

#### `POST /api/user/register` — 注册

请求体：

```json
{
  "username": "zhangsan",
  "password": "abc123456",
  "email": "zhangsan@qq.com"
}
```

#### `POST /api/user/login` — 登录

请求体：

```json
{
  "username": "zhangsan",
  "password": "abc123456"
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "username": "zhangsan",
      "email": "zhangsan@qq.com",
      "avatarUrl": ""
    }
  }
}
```

后续请求在 Header 中携带：`Authorization: Bearer <token>`

---

## 4. 后端项目结构

```
campus-trade-server/
├── src/main/java/com/campustrade/
│   ├── CampusTradeApplication.java      -- Spring Boot 入口
│   ├── config/
│   │   ├── WebConfig.java               -- CORS 跨域配置
│   │   └── MyBatisPlusConfig.java       -- MyBatis-Plus 分页插件
│   ├── controller/
│   │   ├── UserController.java          -- 用户接口
│   │   ├── ProductController.java       -- 商品接口
│   │   ├── MessageController.java       -- 留言接口
│   │   └── BookmarkController.java      -- 收藏接口
│   ├── service/
│   │   ├── UserService.java
│   │   ├── ProductService.java
│   │   ├── MessageService.java
│   │   └── BookmarkService.java
│   ├── service/impl/
│   │   ├── UserServiceImpl.java
│   │   ├── ProductServiceImpl.java
│   │   ├── MessageServiceImpl.java
│   │   └── BookmarkServiceImpl.java
│   ├── mapper/
│   │   ├── UserMapper.java
│   │   ├── ProductMapper.java
│   │   ├── MessageMapper.java
│   │   └── BookmarkMapper.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── Product.java
│   │   ├── Message.java
│   │   └── Bookmark.java
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── ProductRequest.java
│   │   ├── ProductQuery.java
│   │   └── MessageRequest.java
│   ├── common/
│   │   ├── Result.java                  -- 统一响应封装
│   │   ├── PageResult.java              -- 分页响应封装
│   │   └── BusinessException.java       -- 业务异常
│   ├── security/
│   │   ├── JwtUtils.java                -- JWT 工具类
│   │   ├── JwtInterceptor.java          -- JWT 拦截器
│   │   └── LoginUser.java               -- 登录用户上下文
│   └── handler/
│       └── GlobalExceptionHandler.java   -- 全局异常处理
├── src/main/resources/
│   ├── application.yml                  -- 主配置
│   └── db/
│       └── campus_trade.sql             -- 建表 SQL
└── pom.xml                              -- Maven 配置
```

---

## 5. 前端项目结构

```
campus-trade-web/
├── src/
│   ├── main.js                          -- Vue 入口
│   ├── App.vue                          -- 根组件
│   ├── api/
│   │   ├── request.js                   -- axios 封装（拦截器、Token注入）
│   │   ├── user.js                      -- 用户 API
│   │   ├── product.js                   -- 商品 API
│   │   ├── message.js                   -- 留言 API
│   │   └── bookmark.js                  -- 收藏 API
│   ├── router/
│   │   └── index.js                     -- 路由配置
│   ├── store/
│   │   └── user.js                      -- Pinia 用户状态
│   ├── views/
│   │   ├── Home.vue                     -- 首页（商品列表）
│   │   ├── ProductDetail.vue            -- 商品详情
│   │   ├── Publish.vue                  -- 发布商品
│   │   ├── EditProduct.vue              -- 编辑商品
│   │   ├── Login.vue                    -- 登录
│   │   ├── Register.vue                 -- 注册
│   │   ├── Profile.vue                  -- 个人中心
│   │   └── UserProducts.vue             -- 某用户发布的商品
│   ├── components/
│   │   ├── NavBar.vue                   -- 顶部导航栏
│   │   ├── ProductCard.vue              -- 商品卡片
│   │   ├── SearchBar.vue                -- 搜索栏
│   │   ├── CategoryFilter.vue           -- 分类筛选
│   │   ├── MessageSection.vue           -- 留言区
│   │   ├── BookmarkButton.vue           -- 收藏按钮
│   │   └── Pagination.vue               -- 分页组件
│   └── utils/
│       └── constants.js                 -- 分类/成色常量
├── index.html
├── vite.config.js
└── package.json
```

---

## 6. 前后端对接说明

### 6.1 开发模式
- 前端 `npm run dev` → `localhost:5173`
- 后端 Spring Boot → `localhost:8080`
- 前端通过 Vite proxy 将 `/api` 请求代理到后端

### 6.2 前端 Vite 代理配置

```js
// vite.config.js
export default {
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
```

### 6.3 Token 传递
- 登录后 token 存 `localStorage`
- axios 拦截器自动在请求头注入 `Authorization: Bearer <token>`
- 后端 `JwtInterceptor` 从 Header 解析 token，验证后写入 `LoginUser` 上下文

---

## 7. 开发顺序（按天拆解）

| 天 | 任务 | 产出 |
|---|---|---|
| 第 1 天 | 数据库建表 + 后端项目骨架 + 用户模块 | 能注册登录 |
| 第 2 天 | 商品 CRUD 接口 + 列表分页筛选 | 能发布/浏览商品 |
| 第 3 天 | 留言 + 收藏 + 搜索 | 互动功能完成 |
| 第 4 天 | 前端页面（首页、详情、登录注册） | 核心页面能走通 |
| 第 5 天 | 前端页面（发布编辑、个人中心）+ 联调 | 全部功能可跑 |
| 第 6 天 | 部署 + README + 简历文案 | 简历OK，面试链接可用 |

---

## 8. 简历文案（预先写好）

```
[+] 独立开发「校园二手交易平台」全栈项目：
  - 后端 Spring Boot 3 + MyBatis-Plus + MySQL，设计 4 张核心业务表
    及 FULLTEXT 索引优化模糊搜索
  - 前端 Vue 3 + Element Plus，封装 12+ 可复用组件，
    基于 Pinia 管理登录态，axios 拦截器统一注入 JWT
  - 实现分类筛选 + 成色过滤 + 价格排序的组合查询，
    MyBatis-Plus 动态 SQL 拼接，单页 12 条分页加载 < 200ms
  - 自实现 JWT 拦截器 + ThreadLocal 用户上下文，
    基于 BCrypt 密码加密，区分公开/认证接口权限
  - 部署前端至 Vercel、后端至 Railway，支持在线预览
```
