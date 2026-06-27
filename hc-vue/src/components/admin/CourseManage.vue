<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import * as adminApi from '@/api/admin'
import type { Course, Teacher } from '@/types/teacher'
import { useToast } from '@/composables/useToast'

const courses = ref<Course[]>([])
const teachers = ref<Teacher[]>([])
const loading = ref(true)
const error = ref('')
const searchText = ref('')
const showModal = ref(false)
const editingCourse = ref<Course | null>(null)
const form = ref<Course>({ courseId: '', courseName: '', teacherId: '', credit: 0 })
const submitting = ref(false)
const formError = ref('')
const { showSuccess, showError } = useToast()

const filteredCourses = computed(() => {
  if (!searchText.value) return courses.value
  const kw = searchText.value.toLowerCase()
  return courses.value.filter(
    (c) =>
      c.courseId.toLowerCase().includes(kw) ||
      c.courseName.toLowerCase().includes(kw) ||
      c.teacherId.toLowerCase().includes(kw),
  )
})

function getTeacherName(teacherId: string): string {
  const t = teachers.value.find((t) => t.teacherId === teacherId)
  return t ? t.name : teacherId
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [c, t] = await Promise.all([adminApi.getCourses(), adminApi.getTeachers()])
    courses.value = c
    teachers.value = t
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editingCourse.value = null
  form.value = { courseId: '', courseName: '', teacherId: teachers.value[0]?.teacherId || '', credit: 0 }
  formError.value = ''
  showModal.value = true
}

function openEdit(c: Course) {
  editingCourse.value = c
  form.value = { ...c }
  formError.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function handleSubmit() {
  formError.value = ''
  if (!form.value.courseId || !form.value.courseName) {
    formError.value = '课程ID和课程名不能为空'
    return
  }
  submitting.value = true
  try {
    if (editingCourse.value) {
      await adminApi.updateCourse(editingCourse.value.courseId, form.value)
    } else {
      await adminApi.addCourse(form.value)
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

async function handleDelete(c: Course) {
  if (!confirm(`确定要删除课程 "${c.courseName}" (${c.courseId}) 吗？`)) return
  try {
    await adminApi.deleteCourse(c.courseId)
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
      <h2>课程管理</h2>
      <button class="btn btn-primary" @click="openAdd">+ 添加课程</button>
    </div>

    <div class="toolbar">
      <input
        v-model="searchText"
        class="search-input"
        type="text"
        placeholder="搜索课程ID、名称或教师ID..."
      />
    </div>

    <p v-if="loading" class="status">加载中...</p>
    <p v-else-if="error" class="status status-error">{{ error }}</p>
    <p v-else-if="!filteredCourses.length" class="status">暂无数据</p>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>课程ID</th>
          <th>课程名</th>
          <th>教师</th>
          <th>学分</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="c in filteredCourses" :key="c.courseId">
          <td>{{ c.courseId }}</td>
          <td>{{ c.courseName }}</td>
          <td>{{ getTeacherName(c.teacherId) }}</td>
          <td>{{ c.credit }}</td>
          <td class="actions">
            <button class="btn btn-sm" @click="openEdit(c)">编辑</button>
            <button class="btn btn-sm btn-danger" @click="handleDelete(c)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-card">
        <h3>{{ editingCourse ? '编辑课程' : '添加课程' }}</h3>
        <div class="form">
          <label>
            课程ID
            <input
              v-model="form.courseId"
              type="text"
              :disabled="!!editingCourse"
              placeholder="课程ID"
            />
          </label>
          <label>
            课程名
            <input v-model="form.courseName" type="text" placeholder="课程名" />
          </label>
          <label>
            授课教师
            <select v-model="form.teacherId">
              <option v-for="t in teachers" :key="t.teacherId" :value="t.teacherId">
                {{ t.name }} ({{ t.teacherId }})
              </option>
            </select>
          </label>
          <label>
            学分
            <input v-model.number="form.credit" type="number" min="0" />
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
.search-input { padding: 6px 12px; border: 1px solid #d9d9d9; border-radius: 4px; width: 260px; font-size: 14px; }
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
.form input, .form select { padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.form input:disabled { background: #f5f5f5; }
.form-error { color: #ff4d4f; font-size: 13px; margin: 0; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
</style>
