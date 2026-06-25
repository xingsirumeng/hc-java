<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import * as adminApi from '@/api/admin'
import type { Teacher } from '@/types/teacher'

const teachers = ref<Teacher[]>([])
const loading = ref(true)
const error = ref('')
const searchText = ref('')
const showModal = ref(false)
const editingTeacher = ref<Teacher | null>(null)
const form = ref<Teacher>({ teacherId: '', name: '', password: '' })
const submitting = ref(false)
const formError = ref('')

const filteredTeachers = computed(() => {
  if (!searchText.value) return teachers.value
  const kw = searchText.value.toLowerCase()
  return teachers.value.filter(
    (t) =>
      (t.teacherId || '').toLowerCase().includes(kw) ||
      t.name.toLowerCase().includes(kw),
  )
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    teachers.value = await adminApi.getTeachers()
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editingTeacher.value = null
  form.value = { teacherId: '', name: '', password: '' }
  formError.value = ''
  showModal.value = true
}

function openEdit(t: Teacher) {
  editingTeacher.value = t
  form.value = { teacherId: t.teacherId || '', name: t.name, password: '' }
  formError.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function handleSubmit() {
  formError.value = ''
  if (!form.value.teacherId || !form.value.name) {
    formError.value = '教师ID和姓名不能为空'
    return
  }
  if (!editingTeacher.value && !form.value.password) {
    formError.value = '密码不能为空'
    return
  }
  submitting.value = true
  try {
    if (editingTeacher.value) {
      await adminApi.updateTeacher(editingTeacher.value.teacherId!, form.value)
    } else {
      await adminApi.addTeacher(form.value)
    }
    closeModal()
    await load()
  } catch (e: any) {
    formError.value = e?.message || '操作失败'
  } finally {
    submitting.value = false
  }
}

async function handleDelete(t: Teacher) {
  if (!confirm(`确定要删除教师 "${t.name}" (${t.teacherId}) 吗？`)) return
  try {
    await adminApi.deleteTeacher(t.teacherId!)
    await load()
  } catch (e: any) {
    alert(e?.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h2>教师管理</h2>
      <button class="btn btn-primary" @click="openAdd">+ 添加教师</button>
    </div>

    <div class="toolbar">
      <input
        v-model="searchText"
        class="search-input"
        type="text"
        placeholder="搜索教师ID或姓名..."
      />
    </div>

    <p v-if="loading" class="status">加载中...</p>
    <p v-else-if="error" class="status status-error">{{ error }}</p>
    <p v-else-if="!filteredTeachers.length" class="status">暂无数据</p>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>教师ID</th>
          <th>姓名</th>
          <th>密码</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="t in filteredTeachers" :key="t.teacherId">
          <td>{{ t.teacherId }}</td>
          <td>{{ t.name }}</td>
          <td>******</td>
          <td class="actions">
            <button class="btn btn-sm" @click="openEdit(t)">编辑</button>
            <button class="btn btn-sm btn-danger" @click="handleDelete(t)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-card">
        <h3>{{ editingTeacher ? '编辑教师' : '添加教师' }}</h3>
        <div class="form">
          <label>
            教师ID
            <input
              v-model="form.teacherId"
              type="text"
              :disabled="!!editingTeacher"
              placeholder="教师ID"
            />
          </label>
          <label>
            姓名
            <input v-model="form.name" type="text" placeholder="姓名" />
          </label>
          <label>
            {{ editingTeacher ? '新密码（留空不修改）' : '密码' }}
            <input v-model="form.password" type="password" placeholder="密码" />
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
.form input { padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.form input:disabled { background: #f5f5f5; }
.form-error { color: #ff4d4f; font-size: 13px; margin: 0; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
</style>
