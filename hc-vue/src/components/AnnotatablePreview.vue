<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { fetchFileBlob } from '@/api/teacher'
import FilePreview from './FilePreview.vue'

const props = defineProps<{
  filepath: string
  originalFilename: string
}>()

// ── 文件类型检测 ──
const fileType = computed(() => {
  const ext = props.originalFilename.split('.').pop()?.toLowerCase() || ''
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes(ext)) return 'image'
  if (ext === 'pdf') return 'pdf'
  if (ext === 'docx') return 'docx'
  if (['txt', 'md', 'json', 'xml', 'csv', 'log'].includes(ext)) return 'text'
  return 'unsupported'
})

// 只有图片支持标注
const canAnnotate = computed(() => fileType.value === 'image')

// ── Canvas 状态 ──
const canvasRef = ref<HTMLCanvasElement | null>(null)
const containerRef = ref<HTMLDivElement | null>(null)
const ctx = ref<CanvasRenderingContext2D | null>(null)

// 图片 blob URL（用于 Canvas 绘制底图）
const imageBlobUrl = ref('')
const imageElement = ref<HTMLImageElement | null>(null)
const canvasDisplaySize = ref({ w: 0, h: 0 })

// 标注工具状态
type ToolMode = 'pen' | 'text'
const activeTool = ref<ToolMode>('pen')
const penColor = ref('#f5222d')
const penColors = ['#f5222d', '#1890ff', '#52c41a', '#333333']
const penWidth = ref(3)
const penWidths = [
  { label: '细', value: 2 },
  { label: '中', value: 4 },
  { label: '粗', value: 6 },
]

const isDrawing = ref(false)

// 笔迹数据
interface Stroke {
  points: { x: number; y: number }[]
  color: string
  lineWidth: number
}
const strokes = ref<Stroke[]>([])
const currentStroke = ref<{ x: number; y: number }[]>([])

// 文字标注
interface TextAnnotation {
  x: number
  y: number
  text: string
  color: string
  fontSize: number
}
const textAnnotations = ref<TextAnnotation[]>([])

// 文字输入状态
const addingText = ref(false)
const textInputValue = ref('')
const textInputPos = ref({ x: 0, y: 0 })

// 组件加载状态
const loading = ref(false)
const error = ref('')

// ── 加载图片用于 Canvas 底图 ──
async function loadImageForCanvas() {
  loading.value = true
  error.value = ''
  try {
    const { blob } = await fetchFileBlob(props.filepath)
    imageBlobUrl.value = URL.createObjectURL(blob)

    const img = new Image()
    img.src = imageBlobUrl.value
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve()
      img.onerror = () => reject(new Error('图片加载失败'))
    })

    imageElement.value = img

    // 初始化 Canvas 尺寸
    requestAnimationFrame(() => setupCanvas())
  } catch (e: any) {
    error.value = e?.message || '图片加载失败'
  } finally {
    loading.value = false
  }
}

function setupCanvas() {
  if (!canvasRef.value || !containerRef.value || !imageElement.value) return

  const container = containerRef.value
  const img = imageElement.value
  const canvas = canvasRef.value

  // Canvas 显示尺寸 = 容器宽度，高度按图片比例
  const displayW = container.clientWidth
  const ratio = img.naturalHeight / img.naturalWidth
  const displayH = displayW * ratio

  canvasDisplaySize.value = { w: displayW, h: displayH }

  // 设置 Canvas 实际分辨率（2x 高清）
  const dpr = window.devicePixelRatio || 1
  canvas.width = displayW * dpr
  canvas.height = displayH * dpr
  canvas.style.width = displayW + 'px'
  canvas.style.height = displayH + 'px'

  ctx.value = canvas.getContext('2d')!
  ctx.value!.scale(dpr, dpr)

  // 绘制底图 + 所有标注
  redrawAll()
}

function redrawAll() {
  if (!ctx.value || !imageElement.value) return
  const c = ctx.value
  const { w, h } = canvasDisplaySize.value

  // 清空并重绘底图
  c.clearRect(0, 0, w, h)
  c.drawImage(imageElement.value, 0, 0, w, h)

  // 绘制笔迹
  for (const stroke of strokes.value) {
    if (stroke.points.length < 2) continue
    c.beginPath()
    c.strokeStyle = stroke.color
    c.lineWidth = stroke.lineWidth
    c.lineCap = 'round'
    c.lineJoin = 'round'
    c.moveTo(stroke.points[0].x, stroke.points[0].y)
    for (let i = 1; i < stroke.points.length; i++) {
      c.lineTo(stroke.points[i].x, stroke.points[i].y)
    }
    c.stroke()
  }

  // 绘制文字
  for (const ta of textAnnotations.value) {
    c.font = `${ta.fontSize}px Arial`
    c.fillStyle = ta.color
    c.fillText(ta.text, ta.x, ta.y)
  }
}

// ── 坐标转换 ──
function getCanvasPos(e: MouseEvent): { x: number; y: number } {
  const canvas = canvasRef.value!
  const rect = canvas.getBoundingClientRect()
  const scaleX = canvasDisplaySize.value.w / rect.width
  const scaleY = canvasDisplaySize.value.h / rect.height
  return {
    x: (e.clientX - rect.left) * scaleX,
    y: (e.clientY - rect.top) * scaleY,
  }
}

// ── 画笔事件 ──
function onMouseDown(e: MouseEvent) {
  if (!canAnnotate.value) return
  if (activeTool.value === 'text') {
    // 文字模式：记录点击位置，打开文字输入
    const pos = getCanvasPos(e)
    textInputPos.value = pos
    textInputValue.value = ''
    addingText.value = true
    return
  }

  // 画笔模式
  isDrawing.value = true
  currentStroke.value = [getCanvasPos(e)]
}

function onMouseMove(e: MouseEvent) {
  if (!isDrawing.value || !ctx.value) return
  const pos = getCanvasPos(e)
  currentStroke.value.push(pos)

  // 实时绘制当前笔迹：先重绘整体再画当前笔迹
  redrawAll()
  const c = ctx.value
  const pts = currentStroke.value
  if (pts.length < 2) return
  c.beginPath()
  c.strokeStyle = penColor.value
  c.lineWidth = penWidth.value
  c.lineCap = 'round'
  c.lineJoin = 'round'
  c.moveTo(pts[0].x, pts[0].y)
  for (let i = 1; i < pts.length; i++) {
    c.lineTo(pts[i].x, pts[i].y)
  }
  c.stroke()
}

function onMouseUp() {
  if (!isDrawing.value) return
  isDrawing.value = false

  if (currentStroke.value.length > 0) {
    strokes.value.push({
      points: [...currentStroke.value],
      color: penColor.value,
      lineWidth: penWidth.value,
    })
    currentStroke.value = []
    redrawAll()
  }
}

function onMouseLeave() {
  if (isDrawing.value) {
    onMouseUp()
  }
}

// ── 文字输入确认 ──
function confirmText() {
  if (!textInputValue.value.trim()) {
    addingText.value = false
    return
  }

  textAnnotations.value.push({
    x: textInputPos.value.x,
    y: textInputPos.value.y,
    text: textInputValue.value.trim(),
    color: penColor.value,
    fontSize: penWidth.value * 6, // 根据粗细映射字号
  })

  addingText.value = false
  textInputValue.value = ''
  redrawAll()
}

function cancelText() {
  addingText.value = false
  textInputValue.value = ''
}

// ── 工具栏操作 ──
function undo() {
  // 如果是文字模式刚添加的文字，先撤销文字
  if (activeTool.value === 'text' && textAnnotations.value.length > 0) {
    textAnnotations.value.pop()
    redrawAll()
    return
  }
  // 否则撤销笔迹
  if (strokes.value.length > 0) {
    strokes.value.pop()
    redrawAll()
  }
}

function clearAll() {
  strokes.value = []
  currentStroke.value = []
  textAnnotations.value = []
  redrawAll()
}

// ── 导出 ──
async function getAnnotatedBlob(): Promise<Blob | null> {
  if (!canvasRef.value) return null

  return new Promise((resolve) => {
    canvasRef.value!.toBlob((blob) => {
      resolve(blob)
    }, 'image/png')
  })
}

function hasAnnotations(): boolean {
  return strokes.value.length > 0 || textAnnotations.value.length > 0
}

// ── 生命周期 ──
onMounted(() => {
  if (canAnnotate.value) {
    loadImageForCanvas()
  }
})

watch(() => props.filepath, () => {
  if (canAnnotate.value) {
    strokes.value = []
    currentStroke.value = []
    textAnnotations.value = []
    loadImageForCanvas()
  }
})

onUnmounted(() => {
  if (imageBlobUrl.value) URL.revokeObjectURL(imageBlobUrl.value)
})

defineExpose({ getAnnotatedBlob, hasAnnotations, canAnnotate })
</script>

<template>
  <div class="annotatable-preview">
    <!-- 标注工具栏（仅图片） -->
    <div v-if="canAnnotate && !loading && !error" class="toolbar">
      <!-- 工具切换 -->
      <div class="tool-group">
        <button
          class="mode-btn"
          :class="{ active: activeTool === 'pen' }"
          @click="activeTool = 'pen'; addingText = false"
          title="画笔"
        >🖊</button>
        <button
          class="mode-btn"
          :class="{ active: activeTool === 'text' }"
          @click="activeTool = 'text'"
          title="文字"
        >✏</button>
      </div>

      <div class="tool-divider"></div>

      <!-- 颜色 -->
      <div class="tool-group">
        <button
          v-for="c in penColors"
          :key="c"
          class="color-btn"
          :class="{ active: penColor === c }"
          :style="{ background: c }"
          @click="penColor = c"
        ></button>
      </div>

      <div class="tool-divider"></div>

      <!-- 文字模式提示 or 画笔粗细 -->
      <template v-if="activeTool === 'text'">
        <span class="text-hint">点击图片添加文字</span>
      </template>
      <template v-else>
        <div class="tool-group">
          <button
            v-for="pw in penWidths"
            :key="pw.value"
            class="width-btn"
            :class="{ active: penWidth === pw.value }"
            @click="penWidth = pw.value"
          >
            <span class="width-dot" :style="{ width: pw.value + 'px', height: pw.value + 'px' }"></span>
            {{ pw.label }}
          </button>
        </div>
      </template>

      <div class="tool-divider"></div>

      <button
        class="tool-btn"
        @click="undo"
        :disabled="strokes.length === 0 && textAnnotations.length === 0"
        title="撤销"
      >↩</button>
      <button
        class="tool-btn"
        @click="clearAll"
        :disabled="strokes.length === 0 && textAnnotations.length === 0"
        title="清空"
      >🗑</button>
    </div>

    <!-- Canvas 标注区域 -->
    <div v-if="canAnnotate" ref="containerRef" class="canvas-container">
      <div v-if="loading" class="canvas-status">加载图片中...</div>
      <div v-else-if="error" class="canvas-status error">{{ error }}</div>
      <canvas
        v-show="!loading && !error"
        ref="canvasRef"
        class="annotate-canvas"
        :class="{ 'cursor-crosshair': activeTool === 'pen', 'cursor-text': activeTool === 'text' }"
        @mousedown="onMouseDown"
        @mousemove="onMouseMove"
        @mouseup="onMouseUp"
        @mouseleave="onMouseLeave"
      ></canvas>

      <!-- 文字输入弹窗 -->
      <div v-if="addingText" class="text-input-overlay" @click.self="cancelText">
        <div class="text-input-box">
          <input
            ref="textInputRef"
            v-model="textInputValue"
            type="text"
            placeholder="输入批注文字..."
            @keyup.enter="confirmText"
            @keyup.escape="cancelText"
          />
          <div class="text-input-btns">
            <button class="btn-confirm" @click="confirmText">确定</button>
            <button class="btn-cancel-sm" @click="cancelText">取消</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 非图片：回退到普通 FilePreview -->
    <FilePreview
      v-else
      :filepath="filepath"
      :original-filename="originalFilename"
    />
  </div>
</template>

<style scoped>
.annotatable-preview {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ── 工具栏 ── */
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px 6px 0 0;
  flex-shrink: 0;
  flex-wrap: wrap;
  z-index: 10;
}

.tool-group {
  display: flex;
  align-items: center;
  gap: 4px;
}

.tool-divider {
  width: 1px;
  height: 20px;
  background: #ddd;
  margin: 0 4px;
}

.mode-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.mode-btn:hover {
  border-color: #999;
}

.mode-btn.active {
  border-color: #4a90d9;
  background: #e6f7ff;
}

.color-btn {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  padding: 0;
  transition: border-color 0.15s, transform 0.15s;
}

.color-btn:hover {
  transform: scale(1.15);
}

.color-btn.active {
  border-color: #333;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.12);
}

.width-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
  color: #666;
}

.width-btn:hover {
  border-color: #999;
}

.width-btn.active {
  border-color: #4a90d9;
  color: #4a90d9;
  background: #f0f7ff;
}

.width-dot {
  border-radius: 50%;
  background: currentColor;
  display: inline-block;
}

.text-hint {
  font-size: 12px;
  color: #999;
  padding: 0 4px;
}

.tool-btn {
  width: 30px;
  height: 30px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  transition: all 0.15s;
}

.tool-btn:hover:not(:disabled) {
  background: #f5f5f5;
  border-color: #999;
}

.tool-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ── Canvas 区域 ── */
.canvas-container {
  flex: 1;
  min-height: 0;
  overflow: auto;
  position: relative;
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-top: none;
  border-radius: 0 0 6px 6px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
}

.canvas-status {
  padding: 40px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.canvas-status.error {
  color: #f5222d;
}

.annotate-canvas {
  display: block;
  user-select: none;
}

.annotate-canvas.cursor-crosshair {
  cursor: crosshair;
}

.annotate-canvas.cursor-text {
  cursor: text;
}

/* ── 文字输入弹窗 ── */
.text-input-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20;
}

.text-input-box {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  width: 280px;
}

.text-input-box input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
  outline: none;
}

.text-input-box input:focus {
  border-color: #4a90d9;
}

.text-input-btns {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.btn-confirm {
  flex: 1;
  padding: 8px;
  background: #4a90d9;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-confirm:hover {
  background: #357abd;
}

.btn-cancel-sm {
  flex: 1;
  padding: 8px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
}

.btn-cancel-sm:hover {
  background: #e8e8e8;
}
</style>
