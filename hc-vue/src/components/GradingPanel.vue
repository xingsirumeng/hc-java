<script setup lang="ts">
import { ref } from 'vue'
import { gradeSubmission, aiGrade, uploadFile } from '@/api/teacher'
import type { Submission } from '@/types/teacher'
import AnnotatablePreview from './AnnotatablePreview.vue'

const props = defineProps<{
  submission: Submission
  assignmentRequirement?: string | null
}>()

const emit = defineEmits<{
  graded: []
  close: []
}>()

const score = ref<number | null>(props.submission.score)
const comment = ref<string>(props.submission.comment || '')
const loading = ref(false)
const aiLoading = ref(false)
const error = ref('')

// AI 评阅
const showAi = ref(false)
const requirement = ref(props.assignmentRequirement || '')

// 标注预览引用
const annotatableRef = ref<InstanceType<typeof AnnotatablePreview> | null>(null)

async function handleGrade() {
  loading.value = true
  error.value = ''
  try {
    // 如果有标注，上传标注后的图片
    let annotatedFilepath: string | null = null
    if (annotatableRef.value?.canAnnotate && annotatableRef.value?.hasAnnotations()) {
      const blob = await annotatableRef.value.getAnnotatedBlob()
      if (blob) {
        const ext = props.submission.originalFilename.split('.').pop() || 'png'
        const file = new File([blob], 'annotated_' + props.submission.originalFilename, {
          type: 'image/png',
        })
        const uploadResult = await uploadFile(file)
        annotatedFilepath = uploadResult.filepath
      }
    }

    await gradeSubmission(
      props.submission.assignmentName,
      props.submission.studentId,
      score.value,
      comment.value || null,
      annotatedFilepath,
    )
    emit('graded')
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    error.value = '批改失败: ' + detail
  } finally {
    loading.value = false
  }
}

async function handleAiGrade() {
  if (!requirement.value.trim()) {
    error.value = '请输入作业要求'
    return
  }

  aiLoading.value = true
  error.value = ''
  try {
    const result = await aiGrade(props.submission.filepath, requirement.value.trim())
    score.value = result.score
    comment.value = result.commentary
    error.value = ''
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    error.value = 'AI 评阅失败: ' + detail
  } finally {
    aiLoading.value = false
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

      <!-- 学生信息 -->
      <p class="info">学生：{{ submission.studentId }} | 文件：{{ submission.originalFilename }}</p>

      <!-- 并排布局：左侧预览 + 右侧评分 -->
      <div class="modal-body">
        <!-- 左侧：预览（含 Canvas 标注） -->
        <div class="preview-pane">
          <AnnotatablePreview
            ref="annotatableRef"
            :filepath="submission.filepath"
            :original-filename="submission.originalFilename"
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

          <div class="btn-group">
            <button class="btn-save" @click="handleGrade" :disabled="loading">
              {{ loading ? '提交中...' : '💾 保存批改' }}
            </button>
            <button class="btn-cancel" @click="emit('close')">取消</button>
          </div>

          <p v-if="error" class="error-msg">{{ error }}</p>
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
</style>
