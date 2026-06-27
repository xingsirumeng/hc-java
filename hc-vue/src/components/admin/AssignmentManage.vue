<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import * as adminApi from '@/api/admin'
import type { Assignment, Course } from '@/types/teacher'
import { useToast } from '@/composables/useToast'

const assignments = ref<Assignment[]>([])
const courses = ref<Course[]>([])
const loading = ref(true)
const error = ref('')
const searchText = ref('')
const filterCourseId = ref('')
const showModal = ref(false)
const editingAssignment = ref<Assignment | null>(null)
const form = ref<Assignment>({
  name: '',
  startTime: '',
  endTime: '',
  courseId: '',
  requirement: '',
})
const submitting = ref(false)
const formError = ref('')
const { showSuccess, showError } = useToast()

const filteredAssignments = computed(() => {
  let list = assignments.value
  if (filterCourseId.value) {
    list = list.filter((a) => a.courseId === filterCourseId.value)
  }
  if (searchText.value) {
    const kw = searchText.value.toLowerCase()
    list = list.filter(
      (a) =>
        a.name.toLowerCase().includes(kw) ||
        a.courseId.toLowerCase().includes(kw) ||
        (a.requirement && a.requirement.toLowerCase().includes(kw)),
    )
  }
  return list
})

function getCourseName(courseId: string): string {
  const c = courses.value.find((c) => c.courseId === courseId)
  return c ? c.courseName : courseId
}

function formatTime(t: string): string {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN')
}

function toDatetimeLocal(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [a, c] = await Promise.all([adminApi.getAssignments(), adminApi.getCourses()])
    assignments.value = a
    courses.value = c
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editingAssignment.value = null
  form.value = {
    name: '',
    startTime: '',
    endTime: '',
    courseId: courses.value[0]?.courseId || '',
    requirement: '',
  }
  formError.value = ''
  showModal.value = true
}

function openEdit(a: Assignment) {
  editingAssignment.value = a
  form.value = {
    name: a.name,
    startTime: toDatetimeLocal(a.startTime),
    endTime: toDatetimeLocal(a.endTime),
    courseId: a.courseId,
    requirement: a.requirement || '',
  }
  formError.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function handleSubmit() {
  formError.value = ''
  if (!form.value.name || !form.value.courseId) {
    formError.value = '作业名称和课程不能为空'
    return
  }
  submitting.value = true
  try {
    const data = {
      ...form.value,
      startTime: form.value.startTime ? new Date(form.value.startTime).toISOString() : undefined,
      endTime: form.value.endTime ? new Date(form.value.endTime).toISOString() : undefined,
    }
    if (editingAssignment.value) {
      await adminApi.updateAssignment(editingAssignment.value.name, data)
    } else {
      await adminApi.addAssignment(data)
    }
    closeModal()
    await load()
    showSuccess('操作成功')
  } catch (e: any) {
    formError.value = e?.message || '操作失败'
  } finally {
    submitting.value = false
  }
}

async function handleDelete(a: Assignment) {
  if (!confirm(`确定要删除作业 "${a.name}" 吗？`)) return
  try {
    await adminApi.deleteAssignment(a.name)
    await load()
    showSuccess('删除成功')
  } catch (e: any) {
    showError(e?.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h2>作业管理</h2>
      <button class="btn btn-primary" @click="openAdd">+ 添加作业</button>
    </div>

    <div class="toolbar">
      <input
        v-model="searchText"
        class="search-input"
        type="text"
        placeholder="搜索作业名、课程ID或要求..."
      />
      <select v-model="filterCourseId" class="filter-select">
        <option value="">全部课程</option>
        <option v-for="c in courses" :key="c.courseId" :value="c.courseId">
          {{ c.courseName }}
        </option>
      </select>
    </div>

    <p v-if="loading" class="status">加载中...</p>
    <p v-else-if="error" class="status status-error">{{ error }}</p>
    <p v-else-if="!filteredAssignments.length" class="status">暂无数据</p>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>作业名称</th>
          <th>课程</th>
          <th>开始时间</th>
          <th>截止时间</th>
          <th>要求</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="a in filteredAssignments" :key="a.name">
          <td>{{ a.name }}</td>
          <td>{{ getCourseName(a.courseId) }}</td>
          <td>{{ formatTime(a.startTime) }}</td>
          <td>{{ formatTime(a.endTime) }}</td>
          <td class="req-cell">{{ a.requirement || '-' }}</td>
          <td class="actions">
            <button class="btn btn-sm" @click="openEdit(a)">编辑</button>
            <button class="btn btn-sm btn-danger" @click="handleDelete(a)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-card">
        <h3>{{ editingAssignment ? '编辑作业' : '添加作业' }}</h3>
        <div class="form">
          <label>
            作业名称
            <input
              v-model="form.name"
              type="text"
              :disabled="!!editingAssignment"
              placeholder="作业名称"
            />
          </label>
          <label>
            课程
            <select v-model="form.courseId">
              <option v-for="c in courses" :key="c.courseId" :value="c.courseId">
                {{ c.courseName }} ({{ c.courseId }})
              </option>
            </select>
          </label>
          <label>
            开始时间
            <input v-model="form.startTime" type="datetime-local" />
          </label>
          <label>
            截止时间
            <input v-model="form.endTime" type="datetime-local" />
          </label>
          <label>
            作业要求
            <textarea v-model="form.requirement" rows="3" placeholder="作业要求（可选）"></textarea>
          </label>
          <p v-if="formError" class="form-error">{{ formError }}</p>
          <div class="form-actions">
            <button class="btn" @click="closeModal">取消</button>
            <button class="btn btn-primary" :disabled="submitting" @click="handleSubmit">
              {{ submitting ? '提交中...' : '确定' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page { background: #fff; border-radius: 8px; padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; }
.toolbar { margin-bottom: 16px; display: flex; gap: 12px; }
.search-input { padding: 6px 12px; border: 1px solid #d9d9d9; border-radius: 4px; width: 260px; font-size: 14px; }
.filter-select { padding: 6px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.status { text-align: center; color: #999; padding: 40px 0; }
.status-error { color: #ff4d4f; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 10px 12px; border-bottom: 1px solid #f0f0f0; text-align: left; font-size: 13px; }
.data-table th { background: #fafafa; font-weight: 600; }
.req-cell { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.actions { display: flex; gap: 8px; }
.btn { padding: 6px 16px; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; cursor: pointer; font-size: 14px; }
.btn-primary { background: #1890ff; border-color: #1890ff; color: #fff; }
.btn-danger { background: #ff4d4f; border-color: #ff4d4f; color: #fff; }
.btn-sm { padding: 4px 10px; font-size: 13px; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-card { background: #fff; border-radius: 8px; padding: 24px; width: 480px; max-height: 90vh; overflow-y: auto; }
.modal-card h3 { margin: 0 0 16px; }
.form { display: flex; flex-direction: column; gap: 12px; }
.form label { display: flex; flex-direction: column; gap: 4px; font-size: 14px; color: #333; }
.form input, .form select, .form textarea { padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.form input:disabled { background: #f5f5f5; }
.form textarea { resize: vertical; }
.form-error { color: #ff4d4f; font-size: 13px; margin: 0; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
</style>
