<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { fetchFileBlob, downloadFile } from '@/api/teacher'
import mammoth from 'mammoth'

const props = defineProps<{
  filepath: string
  originalFilename: string
}>()

type FileType = 'pdf' | 'image' | 'docx' | 'doc' | 'text' | 'unsupported'

const loading = ref(false)
const error = ref('')
const blobUrl = ref('')
const htmlContent = ref('')
const textContent = ref('')
const fileType = ref<FileType>('unsupported')

function detectFileType(filename: string): FileType {
  const ext = filename.split('.').pop()?.toLowerCase() || ''
  if (ext === 'pdf') return 'pdf'
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes(ext)) return 'image'
  if (ext === 'docx') return 'docx'
  if (ext === 'doc') return 'doc'
  if (['txt', 'md', 'json', 'xml', 'csv', 'log', 'yml', 'yaml', 'properties', 'cfg', 'ini', 'conf'].includes(ext)) return 'text'
  return 'unsupported'
}

function revokeUrl() {
  if (blobUrl.value) {
    URL.revokeObjectURL(blobUrl.value)
    blobUrl.value = ''
  }
}

async function loadPreview() {
  revokeUrl()
  loading.value = true
  error.value = ''
  htmlContent.value = ''
  textContent.value = ''

  const ft = detectFileType(props.originalFilename)
  fileType.value = ft

  try {
    const { blob, contentType } = await fetchFileBlob(props.filepath)

    if (ft === 'text') {
      // 文本文件：读取为文本展示
      textContent.value = await blob.text()
    } else if (ft === 'docx') {
      // DOCX：用 mammoth 转为 HTML
      const arrayBuffer = await blob.arrayBuffer()
      const result = await mammoth.convertToHtml({ arrayBuffer })
      htmlContent.value = result.value
      if (result.messages.length) {
        console.warn('Mammoth conversion messages:', result.messages)
      }
    } else if (ft === 'doc' || ft === 'unsupported') {
      // .doc 或未知类型：不做 blob URL
    } else {
      // PDF / 图片：创建 blob URL 供 iframe/img 使用
      blobUrl.value = URL.createObjectURL(blob)
    }
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    error.value = '加载文件失败: ' + detail
  } finally {
    loading.value = false
  }
}

function handleDownload() {
  downloadFile(props.filepath, props.originalFilename)
}

// 监听 filepath 变化重新加载
watch(() => props.filepath, () => {
  if (props.filepath) loadPreview()
})

onMounted(() => {
  if (props.filepath) loadPreview()
})

onUnmounted(() => {
  revokeUrl()
})
</script>

<template>
  <div class="file-preview">
    <!-- 加载中 -->
    <div v-if="loading" class="preview-status">
      <span class="spinner"></span>
      <p>加载文件预览中...</p>
    </div>

    <!-- 加载失败 -->
    <div v-else-if="error" class="preview-status error">
      <p>⚠️ {{ error }}</p>
      <button class="btn-retry" @click="loadPreview">重试</button>
      <button class="btn-retry btn-dl" @click="handleDownload">📥 下载文件</button>
    </div>

    <!-- PDF -->
    <iframe
      v-else-if="fileType === 'pdf' && blobUrl"
      :src="blobUrl"
      class="preview-iframe"
      frameborder="0"
    ></iframe>

    <!-- 图片 -->
    <div v-else-if="fileType === 'image' && blobUrl" class="preview-image-wrap">
      <img :src="blobUrl" :alt="originalFilename" class="preview-img" />
    </div>

    <!-- DOCX -->
    <div v-else-if="fileType === 'docx' && htmlContent" class="preview-docx-wrap">
      <div class="docx-content" v-html="htmlContent"></div>
    </div>

    <!-- 文本 -->
    <div v-else-if="fileType === 'text' && textContent" class="preview-text-wrap">
      <pre class="preview-text">{{ textContent }}</pre>
    </div>

    <!-- .doc 或其它不支持类型 -->
    <div v-else-if="fileType === 'doc' || fileType === 'unsupported'" class="preview-status unsupported">
      <p v-if="fileType === 'doc'">
        📄 <strong>.doc</strong> 格式暂不支持直接预览（旧版 Word 格式）。<br />请下载后用 Word 打开查看。
      </p>
      <p v-else>
        📁 此文件类型（<strong>{{ originalFilename.split('.').pop() }}</strong>）暂不支持直接预览。
      </p>
      <button class="btn-retry btn-dl" @click="handleDownload">📥 下载文件</button>
    </div>
  </div>
</template>

<style scoped>
.file-preview {
  width: 100%;
  height: 100%;
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border-radius: 6px;
  overflow: hidden;
}

/* 状态提示 */
.preview-status {
  text-align: center;
  color: #888;
  padding: 32px;
}

.preview-status.error {
  color: #f5222d;
}

.preview-status.unsupported {
  color: #666;
}

.preview-status p {
  margin: 0 0 16px;
  font-size: 14px;
  line-height: 1.6;
}

.spinner {
  display: inline-block;
  width: 28px;
  height: 28px;
  border: 3px solid #e0e0e0;
  border-top-color: #4a90d9;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 8px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.btn-retry {
  padding: 6px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  margin: 4px;
}

.btn-retry:hover {
  background: #f0f0f0;
}

.btn-dl {
  background: #4a90d9;
  color: #fff;
  border-color: #4a90d9;
}

.btn-dl:hover {
  background: #357abd;
}

/* PDF */
.preview-iframe {
  width: 100%;
  height: 100%;
  min-height: 500px;
  border: none;
}

/* 图片 */
.preview-image-wrap {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  overflow: auto;
  padding: 8px;
}

.preview-img {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
}

/* DOCX */
.preview-docx-wrap {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  padding: 16px;
}

.docx-content {
  background: #fff;
  padding: 24px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  max-width: 100%;
  line-height: 1.8;
}

/* 文本 */
.preview-text-wrap {
  width: 100%;
  height: 100%;
  overflow: auto;
  padding: 12px;
}

.preview-text {
  margin: 0;
  padding: 16px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  color: #333;
}
</style>
