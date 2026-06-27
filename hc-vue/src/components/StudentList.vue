<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStudentsByCourse, getAssignmentsByCourse, extendDeadline } from '@/api/teacher'
import { useToast } from '@/composables/useToast'
import type { Student, Assignment } from '@/types/teacher'

const { showSuccess, showError } = useToast()

const props = defineProps<{ courseId: string }>()

const students = ref<Student[]>([])
const loading = ref(false)
const error = ref('')

// 延期弹窗
const extendStudentId = ref<string | null>(null)
const assignments = ref<Assignment[]>([])
const extendForm = ref({ assignmentName: '', extendedEndTime: '', reason: '' })
const extendSubmitting = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    students.value = await getStudentsByCourse(props.courseId)
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    error.value = '加载失败: ' + detail
  } finally {
    loading.value = false
  }
}

async function openExtend(studentId: string) {
  extendStudentId.value = studentId
  try {
    assignments.value = await getAssignmentsByCourse(props.courseId)
  } catch {
    assignments.value = []
  }
  extendForm.value = { assignmentName: assignments.value[0]?.name || '', extendedEndTime: '', reason: '' }
}

async function handleExtend() {
  if (!extendStudentId.value || !extendForm.value.assignmentName.trim() || !extendForm.value.extendedEndTime.trim()) {
    showError('请填写完整信息')
    return
  }

  const dt = new Date(extendForm.value.extendedEndTime)
  const dtStr = dt.getFullYear() + '-' +
    String(dt.getMonth() + 1).padStart(2, '0') + '-' +
    String(dt.getDate()).padStart(2, '0') + ' ' +
    String(dt.getHours()).padStart(2, '0') + ':' +
    String(dt.getMinutes()).padStart(2, '0') + ':00'

  extendSubmitting.value = true
  try {
    await extendDeadline(extendForm.value.assignmentName.trim(), extendStudentId.value, dtStr, extendForm.value.reason || undefined)
    showSuccess('延期设置成功')
    extendStudentId.value = null
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    showError('延期设置失败: ' + detail)
  } finally {
    extendSubmitting.value = false
  }
}

defineExpose({ refresh: load })
onMounted(load)
</script>

<template>
  <div class="student-list">
    <h3>课程学生</h3>

    <p v-if="loading" class="status">加载中...</p>
    <p v-else-if="error" class="status error">{{ error }}</p>
    <p v-else-if="!students.length" class="status">暂无学生</p>

    <table v-else>
      <thead>
        <tr>
          <th>学号</th>
          <th>姓名</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="s in students" :key="s.studentId">
          <td>{{ s.studentId }}</td>
          <td>{{ s.name }}</td>
          <td>
            <button class="btn-extend" @click="openExtend(s.studentId)">延期</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 延期弹窗 -->
    <div v-if="extendStudentId" class="modal-overlay" @click.self="extendStudentId = null">
      <div class="modal-card">
        <h3>延长提交时间 — 学生 {{ extendStudentId }}</h3>
        <div class="form">
          <div class="form-group">
            <label>作业</label>
            <select v-model="extendForm.assignmentName">
              <option value="" disabled>请选择作业</option>
              <option v-for="a in assignments" :key="a.name" :value="a.name">{{ a.name }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>延长至</label>
            <input v-model="extendForm.extendedEndTime" type="datetime-local" />
          </div>
          <div class="form-group">
            <label>原因（可选）</label>
            <input v-model="extendForm.reason" type="text" placeholder="延期原因" />
          </div>
          <div class="form-actions">
            <button class="btn-save" :disabled="extendSubmitting" @click="handleExtend">
              {{ extendSubmitting ? '保存中...' : '保存' }}
            </button>
            <button class="btn-cancel" @click="extendStudentId = null">取消</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.student-list {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

h3 {
  margin: 0 0 16px;
  font-size: 16px;
  color: #333;
}

.status {
  font-size: 14px;
  color: #999;
}

.status.error {
  color: #f5222d;
}

.status.success {
  color: #52c41a;
  margin-top: 12px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}

th {
  background: #fafafa;
  color: #555;
}

.btn-extend {
  padding: 3px 10px;
  background: #e6fffb;
  color: #13c2c2;
  border: 1px solid #87e8de;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.btn-extend:hover {
  background: #b5f5ec;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  width: 440px;
  max-width: 90vw;
}

.modal-card h3 {
  margin: 0 0 16px;
  font-size: 16px;
  color: #333;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-group label {
  font-size: 14px;
  color: #555;
}

.form-group input,
.form-group select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.btn-save {
  padding: 8px 20px;
  background: #4a90d9;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.btn-save:hover:not(:disabled) {
  background: #357abd;
}

.btn-save:disabled {
  background: #a0c4e8;
  cursor: not-allowed;
}

.btn-cancel {
  padding: 8px 20px;
  background: #999;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.btn-cancel:hover {
  background: #777;
}
</style>
