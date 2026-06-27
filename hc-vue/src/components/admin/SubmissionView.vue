<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import * as adminApi from '@/api/admin'
import { useToast } from '@/composables/useToast'
import type { Submission } from '@/types/teacher'

const { showSuccess, showError } = useToast()

const submissions = ref<Submission[]>([])
const loading = ref(true)
const error = ref('')
const filterAssignment = ref('')
const filterStudent = ref('')

// 批量删除
const selectedKeys = ref<Set<string>>(new Set())
const selectAll = ref(false)
const batchDeleting = ref(false)

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
    showSuccess('删除成功')
    await load()
  } catch (e: any) {
    showError(e?.message || '删除失败')
  }
}

function toggleSelect(assignmentName: string, studentId: string) {
  const key = `${assignmentName}|${studentId}`
  const s = new Set(selectedKeys.value)
  if (s.has(key)) s.delete(key)
  else s.add(key)
  selectedKeys.value = s
}

function toggleSelectAll() {
  if (selectAll.value) {
    selectedKeys.value = new Set()
    selectAll.value = false
  } else {
    selectedKeys.value = new Set(filteredSubmissions.value.map(s => `${s.assignmentName}|${s.studentId}`))
    selectAll.value = true
  }
}

async function handleBatchDelete() {
  if (selectedKeys.value.size === 0) {
    showError('请选择要删除的提交记录')
    return
  }
  if (!confirm(`确定要删除选中的 ${selectedKeys.value.size} 条提交记录吗？`)) return

  batchDeleting.value = true
  try {
    const items = filteredSubmissions.value
      .filter(s => selectedKeys.value.has(`${s.assignmentName}|${s.studentId}`))
      .map(s => ({ assignmentName: s.assignmentName, studentId: s.studentId }))
    await adminApi.batchDeleteSubmissions(items)
    showSuccess(`批量删除成功，共删除 ${items.length} 条记录`)
    selectedKeys.value = new Set()
    selectAll.value = false
    await load()
  } catch (e: any) {
    showError(e?.message || '批量删除失败')
  } finally {
    batchDeleting.value = false
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
      <button
        v-if="selectedKeys.size > 0"
        class="btn btn-danger btn-sm"
        :disabled="batchDeleting"
        @click="handleBatchDelete"
      >
        {{ batchDeleting ? '删除中...' : `批量删除 (${selectedKeys.size})` }}
      </button>
    </div>

    <p v-if="loading" class="status">加载中...</p>
    <p v-else-if="error" class="status status-error">{{ error }}</p>
    <p v-else-if="!filteredSubmissions.length" class="status">暂无数据</p>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th style="width:36px">
            <input type="checkbox" :checked="selectAll" @change="toggleSelectAll" />
          </th>
          <th>作业名</th>
          <th>学生ID</th>
          <th>状态</th>
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
          <td>
            <input
              type="checkbox"
              :checked="selectedKeys.has(`${s.assignmentName}|${s.studentId}`)"
              @change="toggleSelect(s.assignmentName, s.studentId)"
            />
          </td>
          <td>{{ s.assignmentName }}</td>
          <td>{{ s.studentId }}</td>
          <td>
            <span v-if="s.status === 'returned'" class="status-tag status-returned">已打回</span>
            <span v-else-if="s.score != null" class="status-tag status-graded">已批改</span>
            <span v-else class="status-tag status-submitted">已提交</span>
          </td>
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
.status-success { color: #52c41a; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 10px 12px; border-bottom: 1px solid #f0f0f0; text-align: left; font-size: 13px; }
.data-table th { background: #fafafa; font-weight: 600; }
.file-cell, .comment-cell { max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.actions { display: flex; gap: 8px; }
.btn { padding: 6px 16px; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; cursor: pointer; font-size: 14px; }
.btn-danger { background: #ff4d4f; border-color: #ff4d4f; color: #fff; }
.btn-sm { padding: 4px 10px; font-size: 13px; }
.status-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 500; }
.status-submitted { background: #e6f7ff; color: #1890ff; }
.status-graded { background: #f6ffed; color: #52c41a; }
.status-returned { background: #fff2f0; color: #f5222d; }
</style>
