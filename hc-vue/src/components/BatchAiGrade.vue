<script setup lang="ts">
import { ref } from 'vue'
import { aiGrade, batchGradeSubmissions } from '@/api/teacher'
import { useToast } from '@/composables/useToast'
import type { Submission } from '@/types/teacher'

const { showSuccess, showError } = useToast()

const props = defineProps<{
  submissions: Submission[]
  assignmentName: string
  assignmentRequirement?: string | null
}>()

const emit = defineEmits<{
  graded: []
  close: []
}>()

const requirement = ref(props.assignmentRequirement || '')
const running = ref(false)
const done = ref(false)

interface GradeTask {
  studentId: string
  status: 'pending' | 'grading' | 'done' | 'error'
  score?: number
  commentary?: string
  error?: string
}
const tasks = ref<GradeTask[]>([])

async function start() {
  if (!requirement.value.trim()) {
    showError('请输入作业要求')
    return
  }

  running.value = true
  tasks.value = props.submissions.map(s => ({ studentId: s.studentId, status: 'pending' as const }))

  const grades: Array<{
    assignmentName: string
    studentId: string
    score: number | null
    comment: string | null
    annotatedFilepath: null
    gradeAttachments: null
  }> = []

  for (let i = 0; i < props.submissions.length; i++) {
    const s = props.submissions[i]
    tasks.value[i].status = 'grading'
    try {
      const result = await aiGrade(s.filepath, requirement.value.trim())
      tasks.value[i].status = 'done'
      tasks.value[i].score = result.score
      tasks.value[i].commentary = result.commentary
      grades.push({
        assignmentName: s.assignmentName,
        studentId: s.studentId,
        score: result.score,
        comment: result.commentary,
        annotatedFilepath: null,
        gradeAttachments: null,
      })
    } catch (e: any) {
      tasks.value[i].status = 'error'
      tasks.value[i].error = e?.response?.data?.msg || e?.message || '未知错误'
    }
  }

  if (grades.length > 0) {
    try {
      await batchGradeSubmissions(grades)
    } catch (e: any) {
      showError('保存成绩失败: ' + (e?.response?.data?.msg || e?.message || '未知错误'))
    }
  }

  running.value = false
  done.value = true

  const ok = tasks.value.filter(t => t.status === 'done').length
  const err = tasks.value.filter(t => t.status === 'error').length
  showSuccess(`AI 批改完成：成功 ${ok} 份` + (err > 0 ? `，失败 ${err} 份` : ''))
}
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h2>🤖 批量 AI 批改 — {{ assignmentName }}（{{ submissions.length }} 份）</h2>
        <button class="btn-close-x" @click="emit('close')">✕</button>
      </div>

      <!-- 输入要求 -->
      <div v-if="!running && !done" class="start-section">
        <div class="form-group">
          <label>作业要求（AI 据此评分）</label>
          <textarea v-model="requirement" rows="3" placeholder="请描述作业要求，AI 将据此评分..."></textarea>
        </div>
        <div class="btn-row">
          <button class="btn-start" @click="start">🚀 开始批量 AI 批改</button>
          <button class="btn-cancel" @click="emit('close')">取消</button>
        </div>
      </div>

      <!-- 进度 -->
      <div v-else class="step-progress">
        <p class="progress-title">
          {{ running ? '⏳ AI 批改进行中...' : '✅ AI 批改完成' }}
        </p>

        <div class="task-list">
          <div
            v-for="(t, i) in tasks"
            :key="t.studentId"
            class="task-row"
            :class="'task-' + t.status"
          >
            <span class="task-idx">{{ i + 1 }}</span>
            <span class="task-id">{{ t.studentId }}</span>
            <span class="task-status">
              <template v-if="t.status === 'pending'">等待中</template>
              <template v-else-if="t.status === 'grading'">⏳ AI 评阅中...</template>
              <template v-else-if="t.status === 'done'">✅ {{ t.score }}分</template>
              <template v-else>❌ {{ t.error }}</template>
            </span>
            <span v-if="t.status === 'done' && t.commentary" class="task-comment">{{ t.commentary }}</span>
          </div>
        </div>

        <div class="btn-row" v-if="done">
          <button class="btn-save" @click="emit('graded'); emit('close')">完成</button>
          <button class="btn-cancel" @click="emit('close')">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.35);
  display: flex; align-items: center; justify-content: center; z-index: 100;
}

.modal {
  background: #fff; border-radius: 8px; padding: 24px;
  width: 600px; max-width: 95vw; max-height: 80vh;
  display: flex; flex-direction: column;
}

.modal-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
}

.modal-header h2 { margin: 0; font-size: 18px; }

.btn-close-x {
  background: none; border: none; font-size: 20px;
  cursor: pointer; color: #999; padding: 0 4px;
}
.btn-close-x:hover { color: #333; }

.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; color: #555; }
.form-group textarea {
  width: 100%; padding: 8px 12px; border: 1px solid #ddd;
  border-radius: 4px; font-size: 14px; box-sizing: border-box; resize: vertical;
}

.btn-row { display: flex; gap: 10px; justify-content: flex-end; margin-top: 16px; }

.btn-start {
  padding: 10px 24px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; border: none; border-radius: 4px; font-size: 15px; cursor: pointer;
}
.btn-start:hover { opacity: 0.9; }

.btn-save {
  padding: 10px 24px; background: #4a90d9; color: #fff;
  border: none; border-radius: 4px; font-size: 15px; cursor: pointer;
}

.btn-cancel {
  padding: 10px 24px; background: #999; color: #fff;
  border: none; border-radius: 4px; font-size: 15px; cursor: pointer;
}

.step-progress { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.progress-title { font-size: 15px; color: #333; margin: 0 0 12px; font-weight: 600; }

.task-list {
  flex: 1; overflow-y: auto; border: 1px solid #eee; border-radius: 6px; max-height: 400px;
}

.task-row {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 12px; border-bottom: 1px solid #f5f5f5; font-size: 13px;
}

.task-idx { width: 24px; color: #bbb; flex-shrink: 0; }
.task-id { width: 90px; font-weight: 600; flex-shrink: 0; }
.task-status { width: 130px; flex-shrink: 0; }
.task-comment {
  flex: 1; color: #666; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px;
}

.task-pending { color: #bbb; }
.task-grading { background: #fffbe6; color: #d48806; }
.task-done { background: #f6ffed; color: #389e0d; }
.task-error { background: #fff2f0; color: #cf1322; }
</style>
