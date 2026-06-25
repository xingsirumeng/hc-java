# hc-re — 作业批阅与管理系统

一个完整的作业管理平台，支持教师布置作业、学生提交作业、AI 辅助评阅和 AIGC 检测。

## 项目结构

```
hc-re-java/
├── hc-re/          # 后端 — Spring Boot + MyBatis (Java 21)
├── hc-vue/         # 前端 — Vue 3 + Vite + TypeScript
├── hc-re-AIp/      # AI 服务 — FastAPI + EasyOCR (Python)
└── README.md
```

## 技术栈

| 层 | 技术 |
|---|---|
| 后端框架 | Spring Boot 4.1 (Java 21) |
| ORM | MyBatis (注解方式) |
| 数据库 | PostgreSQL |
| 认证 | JWT + BCrypt |
| 前端框架 | Vue 3 + TypeScript |
| 构建工具 | Vite |
| AI 服务 | FastAPI + EasyOCR + DeepSeek |

## 环境要求

- **Java** 21+
- **Maven** 3.8+
- **Node.js** 18+ (推荐 20+)
- **pnpm** (或 npm)
- **PostgreSQL** (推荐 15+)
- **Python** 3.8+ (AI 服务，可选)

---

## 一、数据库初始化

### 1. 创建数据库

```bash
psql -U postgres -c "CREATE DATABASE hc;"
```

### 2. 建表

```bash
psql -U postgres -d hc -f hc-re/scripts/table.sql
```

### 3. 导入示例数据（可选）

```bash
psql -U postgres -d hc -f hc-re/scripts/insert.sql
```

### 4. 迁移脚本（已有数据库升级用）

如果之前已有旧版数据库，按需执行 `hc-re/scripts/` 中的迁移脚本：
- `migrate_submission_score.sql`
- `migrate_aigc_columns.sql`
- `migrate_assignment_requirement.sql`
- `migrate_annotated_filepath.sql`

---

## 二、后端启动

### 1. 配置数据库连接

编辑 `hc-re/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hc
    username: postgres
    password: 你的密码
```

### 2. 编译运行

```bash
cd hc-re
./mvnw spring-boot:run
```

服务启动在 `http://localhost:8080`，API 前缀为 `/api`。

### 3. 验证

```bash
curl http://localhost:8080/api/hello
# → "Hello World"
```

---

## 三、前端启动

```bash
cd hc-vue

# 安装依赖
pnpm install

# 启动开发服务器（默认 http://localhost:5173）
pnpm dev

# 生产构建
pnpm build
```

前端开发服务器会自动将 `/api` 请求代理到 `http://localhost:8080`（Vite 配置）。

---

## 四、AI 服务启动（可选）

AI 服务提供 AI 评阅和 AIGC 检测功能。不启动也不影响系统基本功能。

### 1. 配置密钥

编辑 `hc-re-AIp/api-data.env`：

```
ZEROTRUE_API_KEY=zt_xxxxxxxxxxxxxxxxxxxxxxxxxxxx
DEEPSEEK_API_KEY=sk-xxxxxxxx
```

### 2. 安装依赖并启动

```bash
cd hc-re-AIp

# 安装依赖
pip install -r requirements.txt

# 启动服务（默认 http://localhost:8000）
python run_http_service.py
```

### 3. 验证

```bash
curl http://localhost:8000/health
# → {"status":"ok", ...}
```

---

## 五、登录使用

### 教师端

浏览器打开 `http://localhost:5173`，自动跳转到登录页。

预置教师账号（示例数据）：
| 教师 ID | 密码 |
|---|---|
| T001 | teacherpass1 |
| T002 | teacherpass2 |
| T003 | teacherpass3 |
| T004 | teacherpass4 |

教师功能：
- 查看自己的课程（每个课程即一个"班"）
- 进入课程查看学生列表和作业列表
- 布置 / 编辑 / 删除作业
- 查看学生提交、预览/下载文件
- 批改作业（手动打分 + 评语）
- AI 辅助评阅
- AIGC 检测
- 个人资料查看修改

### 学生端

访问 `http://localhost:5173/login/student`。

预置学生账号（示例数据）：
| 学号 | 密码 |
|---|---|
| 202201010001 | password123 |
| 202201010002 | password456 |
| 202201010003 | password789 |
| ... | ... |

学生功能：
- 查看已选课程的作业（待提交 / 已逾期 / 已提交）
- 提交作业文件
- 查看提交记录和成绩
- 查看教师评语（长评语点击展开）
- 个人资料查看修改

---

## 系统架构

```
教师 ──→ Vue 前端 ──→ Spring Boot API ──→ PostgreSQL
                          │
学生 ──→ Vue 前端 ──→     │
                          │
                          └──→ FastAPI AI 服务 ──→ DeepSeek / ZeroTrue
```

### 数据库表

| 表 | 说明 |
|---|---|
| `teacher` | 教师（ID、姓名、BCrypt 密码） |
| `student` | 学生（学号、姓名、BCrypt 密码） |
| `courses` | 课程（ID、名称、授课教师、学分） |
| `sc` | 选课关系（学生 ↔ 课程） |
| `assignment` | 作业（名称、开始/截止时间、所属课程、要求） |
| `submission` | 提交记录（作业+学生、文件路径、分数、评语、AIGC 分、批注图片） |

---

## 配置要点

`hc-re/src/main/resources/application.yml` 关键配置：

| 属性 | 默认值 | 说明 |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/hc` | 数据库地址 |
| `server.servlet.context-path` | `/api` | API 路径前缀 |
| `jwt.secret` | `hc-re-teacher-login-secret-key-2026` | JWT 密钥 |
| `jwt.expiration` | `86400000` | Token 有效期（24h） |
| `upload.path` | `./uploads` | 文件上传目录 |
| `spring.servlet.multipart.max-file-size` | `100MB` | 上传文件大小限制 |
"# hc-java" 
