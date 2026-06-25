<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import * as adminApi from '@/api/admin'
import type { ScRecord } from '@/api/admin'
import type { Student, Course } from '@/types/teacher'

const enrollments = ref<ScRecord[]>([])
const students = ref<Student[]>([])
const courses = ref<Course[]>([])
const loading = ref(true)
const error = ref('')
const filterCourseId = ref('')
const showModal = ref(false)
const form = ref({ studentId: '', courseId: '' })
const submitting = ref(false)
const formError = ref('')

const courseOptions = computed(() => {
  if (!filterCourseId.value) return courses.value
  return courses.value.filter((c) => c.courseId === filterCourseId.value)
})

const filteredEnrollments = computed(() => {
  let list = enrollments.value
  if (filterCourseId.value) {
    list = list.filter((e) => e.courseId === filterCourseId.value)
  }
  return list
})

function getStudentName(studentId: string): string {
  const s = students.value.find((s) => s.studentId === studentId)
  return s ? s.name : studentId
}

function getCourseName(courseId: string): string {
  const c = courses.value.find((c) => c.courseId === courseId)
  return c ? c.courseName : courseId
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [e, s, c] = await Promise.all([
      adminApi.getEnrollments(),
      adminApi.getStudents(),
      adminApi.getCourses(),
    ])
    enrollments.value = e
    students.value = s
    courses.value = c
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function openAdd() {
  form.value = {
    studentId: students.value[0]?.studentId || '',
    courseId: filterCourseId.value || courses.value[0]?.courseId || '',
  }
  formError.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function handleSubmit() {
  formError.value = ''
  if (!form.value.studentId || !form.value.courseId) {
    formError.value = '请选择学生和课程'
    return
  }
  submitting.value = true
  try {
    await adminApi.addEnrollment(form.value.studentId, form.value.courseId)
    closeModal()
    await load()
  } catch (e: any) {
    formError.value = e?.message || '操作失败'
  } finally {
    submitting.value = false
  }
}

async function handleDelete(e: ScRecord) {
  const sName = getStudentName(e.studentId)
  const cName = getCourseName(e.courseId)
  if (!confirm(`确定要取消 "${sName}" 的 "${cName}" 选课吗？`)) return
  try {
    await adminApi.deleteEnrollment(e.scId)
    await load()
  } catch (e2: any) {
    alert(e2?.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h2>选课管理</h2>
      <button class="btn btn-primary" @click="openAdd">+ 添加选课</button>
    </div>

    <div class="toolbar">
      <select v-model="filterCourseId" class="filter-select" @change="load">
        <option value="">全部课程</option>
        <option v-for="c in courses" :key="c.courseId" :value="c.courseId">
          {{ c.courseName }}
        </option>
      </select>
    </div>

    <p v-if="loading" class="status">加载中...</p>
    <p v-else-if="error" class="status status-error">{{ error }}</p>
    <p v-else-if="!filteredEnrollments.length" class="status">暂无数据</p>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>选课ID</th>
          <th>学生</th>
          <th>课程</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="e in filteredEnrollments" :key="e.scId">
          <td>{{ e.scId }}</td>
          <td>{{ getStudentName(e.studentId) }} ({{ e.studentId }})</td>
          <td>{{ getCourseName(e.courseId) }} ({{ e.courseId }})</td>
          <td class="actions">
            <button class="btn btn-sm btn-danger" @click="handleDelete(e)">退课</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-card">
        <h3>添加选课</h3>
        <div class="form">
          <label>
            学生
            <select v-model="form.studentId">
              <option v-for="s in students" :key="s.studentId" :value="s.studentId">
                {{ s.name }} ({{ s.studentId }})
              </option>
            </select>
          </label>
          <label>
            课程
            <select v-model="form.courseId">
              <option v-for="c in courses" :key="c.courseId" :value="c.courseId">
                {{ c.courseName }} ({{ c.courseId }})
              </option>
            </select>
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
.toolbar { margin-bottom: 16px; }
.filter-select { padding: 6px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; min-width: 200px; }
.status { text-align: center; color: #999; padding: 40px 0; }
.status-error { color: #ff4d4f; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 10px 12px; border-bottom: 1px solid #f0f0f0; text-align: left; }
.data-table th { background: #fafafa; font-weight: 600; }
.actions { display: flex; gap: 8px; }
.btn { padding: 6px 16px; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; cursor: pointer; font-size: 14px; }
.btn-primary { background: #1890ff; border-color: #1890ff; color: #fff; }
.btn-danger { background: #ff4d4f; border-color: #ff4d4f; color: #fff; }
.btn-sm { padding: 4px 10px; font-size: 13px; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-card { background: #fff; border-radius: 8px; padding: 24px; width: 440px; max-height: 90vh; overflow-y: auto; }
.modal-card h3 { margin: 0 0 16px; }
.form { display: flex; flex-direction: column; gap: 12px; }
.form label { display: flex; flex-direction: column; gap: 4px; font-size: 14px; color: #333; }
.form select { padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.form-error { color: #ff4d4f; font-size: 13px; margin: 0; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
</style>
