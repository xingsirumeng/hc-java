<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import * as adminApi from '@/api/admin'
import type { Submission } from '@/types/teacher'

const submissions = ref<Submission[]>([])
const loading = ref(true)
const error = ref('')
const filterAssignment = ref('')
const filterStudent = ref('')

const filteredSubmissions = computed(() => {
  let list = submissions.value
  if (filterAssignment.value) {
    list = list.filter((s) => s.assignmentName.includes(filterAssignment.value))
  }
  if (filterStudent.value) {
    list = list.filter((s) => s.studentId.includes(filterStudent.value))
  }
  return list
})

function formatTime(t: string): string {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN')
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    submissions.value = await adminApi.getSubmissions()
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function handleDelete(s: Submission) {
  if (!confirm(`确定要删除 ${s.studentId} 的提交记录 "${s.assignmentName}" 吗？`)) return
  try {
    await adminApi.deleteSubmission(s.assignmentName, s.studentId)
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
      <h2>提交记录</h2>
      <span class="count">共 {{ filteredSubmissions.length }} 条</span>
    </div>

    <div class="toolbar">
      <input
        v-model="filterAssignment"
        class="search-input"
        type="text"
        placeholder="按作业名筛选..."
      />
      <input
        v-model="filterStudent"
        class="search-input"
        type="text"
        placeholder="按学生ID筛选..."
      />
    </div>

    <p v-if="loading" class="status">加载中...</p>
    <p v-else-if="error" class="status status-error">{{ error }}</p>
    <p v-else-if="!filteredSubmissions.length" class="status">暂无数据</p>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>作业名</th>
          <th>学生ID</th>
          <th>提交时间</th>
          <th>文件名</th>
          <th>分数</th>
          <th>评语</th>
          <th>AI评分</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="s in filteredSubmissions" :key="`${s.assignmentName}-${s.studentId}`">
          <td>{{ s.assignmentName }}</td>
          <td>{{ s.studentId }}</td>
          <td>{{ formatTime(s.submissionTime) }}</td>
          <td class="file-cell">{{ s.originalFilename || '-' }}</td>
          <td>{{ s.score != null ? s.score : '-' }}</td>
          <td class="comment-cell">{{ s.comment || '-' }}</td>
          <td>{{ s.aigcScore != null ? s.aigcScore : '-' }}</td>
          <td class="actions">
            <button class="btn btn-sm btn-danger" @click="handleDelete(s)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.page { background: #fff; border-radius: 8px; padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; }
.count { color: #999; font-size: 14px; }
.toolbar { margin-bottom: 16px; display: flex; gap: 12px; }
.search-input { padding: 6px 12px; border: 1px solid #d9d9d9; border-radius: 4px; width: 220px; font-size: 14px; }
.status { text-align: center; color: #999; padding: 40px 0; }
.status-error { color: #ff4d4f; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 10px 12px; border-bottom: 1px solid #f0f0f0; text-align: left; font-size: 13px; }
.data-table th { background: #fafafa; font-weight: 600; }
.file-cell, .comment-cell { max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.actions { display: flex; gap: 8px; }
.btn { padding: 6px 16px; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; cursor: pointer; font-size: 14px; }
.btn-danger { background: #ff4d4f; border-color: #ff4d4f; color: #fff; }
.btn-sm { padding: 4px 10px; font-size: 13px; }
</style>
