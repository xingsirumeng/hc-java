# AIGC 检测 & AI 评阅 API 使用指南

## 前置条件

1. 在 `api-data.env` 中配置密钥：

```
# AIGC 检测（ZeroTrue）
ZEROTRUE_API_KEY=zt_xxxxxxxxxxxxxxxxxxxxxxxxxxxx

# AI 评阅（DeepSeek）
DEEPSEEK_API_KEY=sk-xxxxxxxx
```

2. 安装依赖并启动服务：

```bash
pip install -r requirements.txt
python run_http_service.py
```

服务默认运行在 `http://localhost:8000`。

---

## 健康检查

```bash
curl http://localhost:8000/health
```

```json
{
  "status": "ok",
  "ocr_engine": "easyocr",
  "ocr_lang_list": ["ch_sim", "en"],
  "ocr_gpu": false,
  "aigc_available": true,
  "grader_available": true
}
```

---

# 一、AIGC 文本检测

使用 [ZeroTrue](https://zerotrue.com) 检测 AI 生成内容。

## 端点 1.1：文本直接检测

**`POST /api/v1/aigc/detect`**

### 请求

```bash
curl -X POST http://localhost:8000/api/v1/aigc/detect \
  -H "Content-Type: application/json" \
  -d '{"text": "人工智能正在改变我们的生活方式"}'
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| text | string | 是 | 待检测的文本内容（UTF-8） |

### 响应

| 字段 | 类型 | 说明 |
|------|------|------|
| **aigc_score** | integer | **AIGC 率 (0-100)**，越高越可能是 AI 生成 |
| suggestion | string | 处置建议：`Pass` / `Review` / `Block` |
| label | string | 命中标签，`Aigc` = AI 生成，`Normal` = 正常 |
| detail_results | array | 各维度详细检测结果 |
| request_id | string | 唯一请求 ID |
| raw | object | ZeroTrue 完整原始响应 |

```json
{
  "aigc_score": 85,
  "suggestion": "Review",
  "label": "Aigc",
  "detail_results": [
    {
      "Label": "Aigc",
      "Score": 85,
      "Suggestion": "Review"
    }
  ],
  "request_id": "check_abc123...",
  "raw": { ... }
}
```

---

## 端点 1.2：文件上传检测

**`POST /api/v1/aigc/detect/upload`**

上传文件 → 自动提取文本 → AIGC 检测。

### 支持的文件类型

| 类型 | 后缀 |
|------|------|
| 纯文本 | .txt, .md, .csv, .json, .xml, .py, .java, .js, .html, .css ... |
| Word 文档 | .docx |
| 图片（OCR） | .png, .jpg, .jpeg, .bmp, .tiff, .webp ... |

### 请求

```bash
curl -X POST http://localhost:8000/api/v1/aigc/detect/upload \
  -F "file=@文档.txt"
```

### 响应

```json
{
  "file_name": "文档.txt",
  "file_type": "txt",
  "extracted_text": "人工智能正在改变我们的生活方式",
  "aigc_result": {
    "aigc_score": 85,
    "suggestion": "Review",
    "label": "Aigc",
    "detail_results": [...],
    "request_id": "check_abc123...",
    "raw": { ... }
  }
}
```

---

# 二、AI 作业评阅

传入「作业要求」+「学生提交」，AI 返回 **0-100 的评分** 和 **中文评语**。

## 端点 2.1：文本直接评阅

**`POST /api/v1/grade`**

### 请求

```bash
curl -X POST http://localhost:8000/api/v1/grade \
  -H "Content-Type: application/json" \
  -d '{
    "assignment": "请写一篇300字左右的短文，论述人工智能对教育的影响",
    "text": "人工智能正在深刻改变教育领域..."
  }'
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| assignment | string | 是 | 作业要求 / 问题描述 |
| text | string | 是 | 学生提交的文本内容 |

### 响应

| 字段 | 类型 | 说明 |
|------|------|------|
| **score** | integer | **评分 (0-100)** |
| **commentary** | string | **中文评语**（优点 + 不足 + 改进建议） |
| model | string | 使用的 AI 模型 |
| raw | object | DeepSeek API 原始响应 |

```json
{
  "score": 82,
  "commentary": "优点：文章结构清晰，能够从多个角度分析人工智能对教育的影响... 不足：案例不够具体，缺乏数据支撑... 改进建议：可以加入具体的AI教育应用实例...",
  "model": "deepseek-chat",
  "raw": { ... }
}
```

---

## 端点 2.2：文件上传评阅

**`POST /api/v1/grade/upload`**

上传文件 → 自动提取文本 → 结合作业要求评阅。

### 请求

```bash
curl -X POST http://localhost:8000/api/v1/grade/upload \
  -F "assignment=请写一篇关于人工智能对教育影响的短文" \
  -F "file=@学生作业.txt"
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| assignment | form field | 是 | 作业要求 / 问题描述 |
| file | file | 是 | 学生提交的文件 |

### 响应

```json
{
  "file_name": "学生作业.txt",
  "file_type": "txt",
  "extracted_text": "人工智能正在深刻改变教育领域...",
  "score": 82,
  "commentary": "优点：... 不足：... 改进建议：...",
  "model": "deepseek-chat",
  "raw": { ... }
}
```

---

## 评分标准（自带 System Prompt）

| 分数 | 等级 | 说明 |
|------|------|------|
| 90-100 | 优秀 | 完全满足要求，有深度和创见 |
| 80-89 | 良好 | 满足大部分要求，思路清晰 |
| 70-79 | 中等 | 基本满足要求但有明显不足 |
| 60-69 | 及格 | 勉强满足要求，存在较多问题 |
| 0-59 | 不及格 | 未达到基本要求 |

---

# 三、错误处理

| 状态码 | 说明 |
|--------|------|
| 503 | 未配置对应密钥 |
| 400 | 参数缺失 / 文件为空 |
| 200 + error 字段 | 处理过程中出错（文本提取失败等） |

```json
// 503 - 未配置密钥
{ "detail": "AIGC detection is not configured (missing ZEROTRUE_API_KEY)" }

// 200 - 文件无可读文本
{ "error": "no text extracted", "details": "the file contained no readable text" }
```

---

# 四、完整测试脚本

```bash
#!/bin/bash
BASE="http://localhost:8000"

echo "=== 健康检查 ==="
curl -s $BASE/health | python -m json.tool

echo -e "\n=== AIGC 文本检测 ==="
curl -s -X POST $BASE/api/v1/aigc/detect \
  -H "Content-Type: application/json" \
  -d '{"text": "人工智能正在改变世界"}' | python -m json.tool

echo -e "\n=== AI 作业评阅 ==="
curl -s -X POST $BASE/api/v1/grade \
  -H "Content-Type: application/json" \
  -d '{"assignment": "写一篇关于AI的短文", "text": "AI正在改变世界..."}' | python -m json.tool

echo -e "\n=== 文件上传评阅 ==="
echo "AI技术近年来发展迅速，深度学习、自然语言处理等技术不断突破。" > /tmp/test_submission.txt
curl -s -X POST $BASE/api/v1/grade/upload \
  -F "assignment=请写一篇关于AI技术发展的短文" \
  -F "file=@/tmp/test_submission.txt" | python -m json.tool
```
