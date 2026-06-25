<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAllTeachers, getTeacherById, deleteTeacher } from '@/api/teacher'
import type { Teacher } from '@/types/teacher'

const emit = defineEmits<{
  edit: [teacher: Teacher]
}>()

const teachers = ref<Teacher[]>([])
const loading = ref(false)
const error = ref('')
const searchId = ref<string>()

async function loadTeachers() {
  loading.value = true
  error.value = ''

  try {
    teachers.value = await getAllTeachers()
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    error.value = '加载失败: ' + detail
  } finally {
    loading.value = false
  }
}

async function searchById() {
  if (!searchId.value) return

  loading.value = true
  error.value = ''

  try {
    const teacher = await getTeacherById(searchId.value)
    teachers.value = teacher ? [teacher] : []
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    error.value = '搜索失败: ' + detail
  } finally {
    loading.value = false
  }
}

function clearSearch() {
  searchId.value = undefined
  loadTeachers()
}

async function handleDelete(teacher: Teacher) {
  if (!confirm(`确定要删除教师 ${teacher.name}（ID: ${teacher.teacherId}）吗？`)) return

  try {
    await deleteTeacher(teacher.teacherId!)
    loadTeachers()
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    error.value = '删除失败: ' + detail
  }
}

function handleEdit(teacher: Teacher) {
  emit('edit', { ...teacher })
}

defineExpose({ refresh: loadTeachers })

onMounted(loadTeachers)
</script>

<template>
  <div class="teacher-list">
    <h2>教师列表</h2>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <input
        v-model="searchId"
        type="text"
        placeholder="输入教师 ID 搜索"
        @keyup.enter="searchById"
      />
      <button class="btn-search" @click="searchById">搜索</button>
      <button class="btn-clear" @click="clearSearch">显示全部</button>
    </div>

    <p v-if="loading" class="status">加载中...</p>
    <p v-else-if="error" class="status error">{{ error }}</p>
    <p v-else-if="!teachers.length" class="status">暂无教师数据</p>

    <table v-else>
      <thead>
        <tr>
          <th>ID</th>
          <th>姓名</th>
          <th>密码</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="t in teachers" :key="t.teacherId">
          <td>{{ t.teacherId }}</td>
          <td>{{ t.name }}</td>
          <td>{{ t.password }}</td>
          <td class="actions">
            <button class="btn-edit" @click="handleEdit(t)">编辑</button>
            <button class="btn-delete" @click="handleDelete(t)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.teacher-list {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.teacher-list h2 {
  margin: 0 0 20px;
  font-size: 18px;
  color: #333;
}

.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.search-bar input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.search-bar input:focus {
  outline: none;
  border-color: #4a90d9;
}

.btn-search {
  padding: 8px 16px;
  background: #4a90d9;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}

.btn-search:hover {
  background: #357abd;
}

.btn-clear {
  padding: 8px 16px;
  background: #999;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}

.btn-clear:hover {
  background: #777;
}

.status {
  font-size: 14px;
  color: #999;
}

.status.error {
  color: #f5222d;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}

th {
  background: #fafafa;
  color: #555;
  font-weight: 600;
}

tr:hover td {
  background: #f5f9ff;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn-edit {
  padding: 4px 12px;
  background: #faad14;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

.btn-edit:hover {
  background: #d48806;
}

.btn-delete {
  padding: 4px 12px;
  background: #f5222d;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

.btn-delete:hover {
  background: #cf1322;
}
</style>
