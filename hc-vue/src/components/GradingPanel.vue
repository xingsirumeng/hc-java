<script setup lang="ts">
import { ref, computed } from 'vue'
import { gradeSubmission, aiGrade, uploadFile, returnSubmission, extendDeadline } from '@/api/teacher'
import type { Submission } from '@/types/teacher'
import { useToast } from '@/composables/useToast'
import AnnotatablePreview from './AnnotatablePreview.vue'

const { showSuccess, showError } = useToast()

const props = defineProps<{
  submission: Submission
  assignmentRequirement?: string | null
  allSubmissions?: Submission[]
}>()

const emit = defineEmits<{
  graded: []
  close: []
  navigate: [submission: Submission]
}>()

const currentIndex = ref(0)
if (props.allSubmissions && props.allSubmissions.length > 1) {
  currentIndex.value = props.allSubmissions.findIndex(
    s => s.assignmentName === props.submission.assignmentName && s.studentId === props.submission.studentId
  )
  if (currentIndex.value < 0) currentIndex.value = 0
}

const hasPrev = ref(false)
const hasNext = ref(false)
function updateNavState() {
  if (props.allSubmissions && props.allSubmissions.length > 1) {
    hasPrev.value = currentIndex.value > 0
    hasNext.value = currentIndex.value < props.allSubmissions.length - 1
  }
}
updateNavState()

const currentSubmission = computed(() => {
  if (props.allSubmissions && props.allSubmissions.length > 0) {
    return props.allSubmissions[currentIndex.value] || props.submission
  }
  return props.submission
})

function goPrev() {
  if (!props.allSubmissions || currentIndex.value <= 0) return
  currentIndex.value--
  updateNavState()
  const sub = props.allSubmissions[currentIndex.value]
  resetForm(sub)
  emit('navigate', sub)
}

function goNext() {
  if (!props.allSubmissions || currentIndex.value >= props.allSubmissions.length - 1) return
  currentIndex.value++
  updateNavState()
  const sub = props.allSubmissions[currentIndex.value]
  resetForm(sub)
  emit('navigate', sub)
}

function resetForm(sub: Submission) {
  score.value = sub.score
  comment.value = sub.comment || ''
  showReturnForm.value = false
  returnReason.value = ''
  showExtendForm.value = false
  extendEndTime.value = ''
  extendReason.value = ''
  gradeAttachments.value = []
  try {
    if (sub.gradeAttachments) {
      gradeAttachments.value = JSON.parse(sub.gradeAttachments)
    }
  } catch (_) {
    gradeAttachments.value = []
  }
}

const score = ref<number | null>(props.submission.score)
const comment = ref<string>(props.submission.comment || '')
const loading = ref(false)
const aiLoading = ref(false)

// AI 评阅
const showAi = ref(false)
const requirement = ref(props.assignmentRequirement || '')

// 标注预览引用
const annotatableRef = ref<InstanceType<typeof AnnotatablePreview> | null>(null)

// 批改附件
interface GradeAttachment {
  filename: string
  filepath: string
}
const gradeAttachments = ref<GradeAttachment[]>([])
const attachmentUploading = ref(false)

// 解析已有附件
try {
  if (currentSubmission.value.gradeAttachments) {
    gradeAttachments.value = JSON.parse(currentSubmission.value.gradeAttachments)
  }
} catch (_) {
  gradeAttachments.value = []
}

// 打回作业
const returnReason = ref('')
const showReturnForm = ref(false)
const returning = ref(false)

// 延期
const showExtendForm = ref(false)
const extendEndTime = ref('')
const extendReason = ref('')
const extendSubmitting = ref(false)

async function handleUploadAttachment(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files?.length) return

  attachmentUploading.value = true
  try {
    const file = input.files[0] as File
    const result = await uploadFile(file)
    gradeAttachments.value.push({
      filename: result.originalFilename,
      filepath: result.filepath,
    })
    input.value = ''
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    showError('附件上传失败: ' + detail)
  } finally {
    attachmentUploading.value = false
  }
}

function removeAttachment(index: number) {
  gradeAttachments.value.splice(index, 1)
}

async function handleGrade() {
  loading.value = true
  try {
    let annotatedFilepath: string | null = null
    if (annotatableRef.value?.canAnnotate && annotatableRef.value?.hasAnnotations()) {
      const blob = await annotatableRef.value.getAnnotatedBlob()
      if (blob) {
        const ext = currentSubmission.value.originalFilename.split('.').pop() || 'png'
        const file = new File([blob], 'annotated_' + currentSubmission.value.originalFilename, {
          type: 'image/png',
        })
        const uploadResult = await uploadFile(file)
        annotatedFilepath = uploadResult.filepath
      }
    }

    const attachmentsJson = gradeAttachments.value.length > 0
      ? JSON.stringify(gradeAttachments.value)
      : null

    await gradeSubmission(
      currentSubmission.value.assignmentName,
      currentSubmission.value.studentId,
      score.value,
      comment.value || null,
      annotatedFilepath,
      attachmentsJson,
    )
    showSuccess('批改成功')
    emit('graded')
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    showError('批改失败: ' + detail)
  } finally {
    loading.value = false
  }
}

async function handleAiGrade() {
  if (!requirement.value.trim()) {
    showError('请输入作业要求')
    return
  }

  aiLoading.value = true
  try {
    const result = await aiGrade(currentSubmission.value.filepath, requirement.value.trim())
    score.value = result.score
    comment.value = result.commentary
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    showError('AI 评阅失败: ' + detail)
  } finally {
    aiLoading.value = false
  }
}

async function handleReturn() {
  if (!returnReason.value.trim()) {
    showError('请输入打回原因')
    return
  }

  returning.value = true
  try {
    await returnSubmission(
      currentSubmission.value.assignmentName,
      currentSubmission.value.studentId,
      returnReason.value.trim(),
    )
    showSuccess('作业已打回')
    emit('graded')
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    showError('打回失败: ' + detail)
  } finally {
    returning.value = false
  }
}

async function handleExtendDeadline() {
  if (!extendEndTime.value.trim()) {
    showError('请选择延长后的截止时间')
    return
  }
  const dt = new Date(extendEndTime.value)
  const dtStr = dt.getFullYear() + '-' +
    String(dt.getMonth() + 1).padStart(2, '0') + '-' +
    String(dt.getDate()).padStart(2, '0') + ' ' +
    String(dt.getHours()).padStart(2, '0') + ':' +
    String(dt.getMinutes()).padStart(2, '0') + ':00'

  extendSubmitting.value = true
  try {
    await extendDeadline(
      currentSubmission.value.assignmentName,
      currentSubmission.value.studentId,
      dtStr,
      extendReason.value || undefined,
    )
    showSuccess('延期设置成功')
    showExtendForm.value = false
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    showError('延期设置失败: ' + detail)
  } finally {
    extendSubmitting.value = false
  }
}
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="modal">
      <!-- 标题栏 -->
      <div class="modal-header">
        <h2>批改作业</h2>
        <button class="btn-close-x" @click="emit('close')">✕</button>
      </div>

      <!-- 导航 + 学生信息 -->
      <div class="nav-bar">
        <button class="btn-nav" :disabled="!hasPrev" @click="goPrev" title="上一个">◀ 上一个</button>
        <span class="nav-info">
          学生：{{ currentSubmission.studentId }} | 文件：{{ currentSubmission.originalFilename }}
          <span v-if="allSubmissions && allSubmissions.length > 1" class="nav-pos">
            ({{ currentIndex + 1 }}/{{ allSubmissions.length }})
          </span>
        </span>
        <button class="btn-nav" :disabled="!hasNext" @click="goNext" title="下一个">下一个 ▶</button>
      </div>

      <!-- 并排布局：左侧预览 + 右侧评分 -->
      <div class="modal-body">
        <!-- 左侧：预览（含 Canvas 标注） -->
        <div class="preview-pane">
          <AnnotatablePreview
            ref="annotatableRef"
            :filepath="currentSubmission.filepath"
            :original-filename="currentSubmission.originalFilename"
          />
        </div>

        <!-- 右侧：评分表单 -->
        <div class="grade-pane">
          <!-- AI 评阅区域 -->
          <div class="ai-section">
            <button
              v-if="!showAi"
              class="btn-ai"
              @click="showAi = true"
            >🤖 AI 评阅</button>

            <div v-else class="ai-panel">
              <div class="form-group">
                <label>作业要求（AI 据此评分）</label>
                <textarea
                  v-model="requirement"
                  rows="2"
                  placeholder="例如：请写一篇300字左右的短文，论述人工智能对教育的影响"
                ></textarea>
              </div>
              <button class="btn-ai-submit" @click="handleAiGrade" :disabled="aiLoading">
                {{ aiLoading ? 'AI 评阅中...' : '🤖 开始 AI 评阅' }}
              </button>
              <button class="btn-ai-cancel" @click="showAi = false">收起</button>
            </div>
          </div>

          <div class="form-group">
            <label>分数 (0-100)</label>
            <input v-model.number="score" type="number" min="0" max="100" placeholder="0-100" />
          </div>

          <div class="form-group">
            <label>评语</label>
            <textarea v-model="comment" rows="4" placeholder="输入评语..."></textarea>
          </div>

          <!-- 批改附件 -->
          <div class="form-group">
            <label>批改附件（可选，支持多个文件）</label>
            <div class="attachment-list">
              <div v-for="(att, idx) in gradeAttachments" :key="idx" class="attachment-item">
                <span class="att-name">{{ att.filename }}</span>
                <button class="att-remove" @click="removeAttachment(idx)" title="移除">✕</button>
              </div>
            </div>
            <label class="btn-upload-attach" :class="{ disabled: attachmentUploading }">
              {{ attachmentUploading ? '上传中...' : '📎 添加附件' }}
              <input
                type="file"
                @change="handleUploadAttachment"
                :disabled="attachmentUploading"
                style="display:none"
              />
            </label>
          </div>

          <div class="btn-group">
            <button class="btn-save" @click="handleGrade" :disabled="loading">
              {{ loading ? '提交中...' : '💾 保存批改' }}
            </button>
            <button
              class="btn-return-toggle"
              @click="showReturnForm = !showReturnForm"
              type="button"
            >
              {{ showReturnForm ? '取消打回' : '↩ 打回作业' }}
            </button>
            <button class="btn-cancel" @click="emit('close')">取消</button>
          </div>

          <!-- 打回作业表单 -->
          <div v-if="showReturnForm" class="return-form">
            <div class="form-group">
              <label>打回原因</label>
              <textarea
                v-model="returnReason"
                rows="2"
                placeholder="请输入打回原因，学生将看到此信息..."
              ></textarea>
            </div>
            <button
              class="btn-return"
              @click="handleReturn"
              :disabled="returning"
            >
              {{ returning ? '打回中...' : '确认打回' }}
            </button>
          </div>

          <!-- 延期设置 -->
          <div class="extend-section">
            <button
              class="btn-extend-toggle"
              @click="showExtendForm = !showExtendForm"
              type="button"
            >
              {{ showExtendForm ? '取消延期' : '⏰ 延长提交时间' }}
            </button>
            <div v-if="showExtendForm" class="extend-form">
              <div class="form-group">
                <label>延长至</label>
                <input v-model="extendEndTime" type="datetime-local" />
              </div>
              <div class="form-group">
                <label>原因（可选）</label>
                <input v-model="extendReason" type="text" placeholder="延期原因" />
              </div>
              <button
                class="btn-extend-save"
                @click="handleExtendDeadline"
                :disabled="extendSubmitting"
              >
                {{ extendSubmitting ? '保存中...' : '确认延期' }}
              </button>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  width: 960px;
  max-width: 95vw;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.modal-header h2 {
  margin: 0;
  font-size: 18px;
}

.btn-close-x {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
  padding: 0 4px;
}

.btn-close-x:hover {
  color: #333;
}

.info {
  margin: 0 0 16px;
  color: #888;
  font-size: 13px;
}

/* 导航栏 */
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}

.nav-info {
  color: #888;
  font-size: 13px;
  text-align: center;
  flex: 1;
}

.nav-pos {
  color: #bbb;
  font-size: 12px;
  margin-left: 4px;
}

.btn-nav {
  padding: 5px 14px;
  background: #f5f5f5;
  color: #4a90d9;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}

.btn-nav:hover:not(:disabled) {
  background: #e6f7ff;
  border-color: #91d5ff;
}

.btn-nav:disabled {
  color: #ccc;
  cursor: not-allowed;
  background: #fafafa;
}

/* 并排布局 */
.modal-body {
  display: flex;
  gap: 20px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.preview-pane {
  flex: 1 1 55%;
  min-width: 0;
  overflow: hidden;
  border-radius: 6px;
  display: flex;
}

.preview-pane > :deep(*) {
  flex: 1;
  min-height: 0;
}

.grade-pane {
  flex: 0 0 280px;
  width: 280px;
  overflow-y: auto;
  padding-right: 4px;
}

@media (max-width: 768px) {
  .modal-body {
    flex-direction: column;
  }

  .preview-pane {
    flex: 0 0 350px;
    max-height: 350px;
  }

  .grade-pane {
    flex: 1 1 auto;
    width: 100%;
  }
}

/* AI 评阅 */
.ai-section {
  margin-bottom: 16px;
}

.btn-ai {
  width: 100%;
  padding: 10px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 15px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-ai:hover {
  opacity: 0.9;
}

.ai-panel {
  background: #f9f5ff;
  border: 1px solid #d3adf7;
  border-radius: 6px;
  padding: 12px;
}

.btn-ai-submit {
  width: 100%;
  padding: 8px;
  background: #722ed1;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.btn-ai-submit:disabled {
  background: #b37feb;
  cursor: not-allowed;
}

.btn-ai-cancel {
  width: 100%;
  margin-top: 6px;
  padding: 6px;
  background: transparent;
  color: #888;
  border: none;
  font-size: 13px;
  cursor: pointer;
}

/* 表单 */
.form-group {
  margin-bottom: 14px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #555;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

/* 按钮组 */
.btn-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.btn-save {
  padding: 10px;
  background: #4a90d9;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 15px;
  cursor: pointer;
}

.btn-save:disabled {
  background: #a0c4e8;
  cursor: not-allowed;
}

.btn-cancel {
  padding: 10px;
  background: #999;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 15px;
  cursor: pointer;
}

.btn-cancel:hover {
  background: #777;
}

.error-msg {
  margin: 12px 0 0;
  color: #f5222d;
  font-size: 14px;
}

.success-msg {
  margin: 12px 0 0;
  color: #52c41a;
  font-size: 14px;
}

/* 批改附件 */
.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f5f5f5;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 12px;
}

.att-name {
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.att-remove {
  background: none;
  border: none;
  color: #f5222d;
  cursor: pointer;
  font-size: 14px;
  padding: 0 2px;
  flex-shrink: 0;
}

.btn-upload-attach {
  display: inline-block;
  padding: 6px 12px;
  background: #f5f5f5;
  border: 1px dashed #ccc;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-upload-attach:hover {
  background: #e8e8e8;
}

.btn-upload-attach.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 打回作业 */
.btn-return-toggle {
  padding: 10px;
  background: #fff;
  color: #faad14;
  border: 1px solid #faad14;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.btn-return-toggle:hover {
  background: #fffbe6;
}

.return-form {
  margin-top: 12px;
  padding: 12px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
}

.btn-return {
  width: 100%;
  padding: 8px;
  background: #faad14;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.btn-return:hover:not(:disabled) {
  background: #d48806;
}

.btn-return:disabled {
  background: #ffe58f;
  cursor: not-allowed;
}

/* 延期设置 */
.extend-section {
  margin-top: 12px;
}

.btn-extend-toggle {
  width: 100%;
  padding: 8px;
  background: #fff;
  color: #13c2c2;
  border: 1px solid #87e8de;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
}

.btn-extend-toggle:hover { background: #e6fffb; }

.extend-form {
  margin-top: 8px;
  padding: 12px;
  background: #e6fffb;
  border: 1px solid #87e8de;
  border-radius: 6px;
}

.btn-extend-save {
  width: 100%;
  padding: 8px;
  background: #13c2c2;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.btn-extend-save:hover:not(:disabled) { background: #08979c; }
.btn-extend-save:disabled { background: #87e8de; cursor: not-allowed; }
</style>
