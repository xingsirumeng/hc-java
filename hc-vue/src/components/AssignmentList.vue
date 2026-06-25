<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAssignmentsByCourse, getSubmissions, downloadFile, previewFile, aiAigcDetect, deleteAssignment } from '@/api/teacher'
import type { Assignment, Submission } from '@/types/teacher'
import GradingPanel from './GradingPanel.vue'

const props = defineProps<{ courseId: string }>()

const emit = defineEmits<{
  edit: [assignment: Assignment]
}>()

const assignments = ref<Assignment[]>([])
const loading = ref(false)
const error = ref('')

// 展开的作业 → 提交列表
const expandedAssignment = ref<string | null>(null)
const submissions = ref<Submission[]>([])
const subsLoading = ref(false)

// 批改面板
const gradingTarget = ref<Submission | null>(null)
const gradingAssignment = ref<Assignment | null>(null)

// AIGC 检测结果：key = studentId
const aigcResults = ref<Record<string, { score: number; label: string; loading: boolean }>>({})

async function detectAigc(sub: Submission) {
  aigcResults.value = {
    ...aigcResults.value,
    [sub.studentId]: { score: 0, label: '', loading: true },
  }
  try {
    const result = await aiAigcDetect(sub.filepath, sub.assignmentName, sub.studentId)
    const score = result.aigc_score
    const label = result.label
    aigcResults.value = {
      ...aigcResults.value,
      [sub.studentId]: { score, label, loading: false },
    }
  } catch {
    aigcResults.value = {
      ...aigcResults.value,
      [sub.studentId]: { score: -1, label: '错误', loading: false },
    }
  }
}

/** 从 sub 或检测结果中获取 AIGC 分数 */
function getAigcScore(sub: Submission): number | null {
  if (sub.aigcScore != null) return sub.aigcScore
  const r = aigcResults.value[sub.studentId]
  if (r && !r.loading && r.score >= 0) return r.score
  return null
}

function isAigcDetecting(sub: Submission): boolean {
  return aigcResults.value[sub.studentId]?.loading === true
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    assignments.value = await getAssignmentsByCourse(props.courseId)
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    error.value = '加载失败: ' + detail
  } finally {
    loading.value = false
  }
}

async function toggleSubmissions(a: Assignment) {
  if (expandedAssignment.value === a.name) {
    expandedAssignment.value = null
    submissions.value = []
    return
  }

  expandedAssignment.value = a.name
  subsLoading.value = true
  try {
    submissions.value = await getSubmissions(a.name)
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    error.value = '加载提交失败: ' + detail
  } finally {
    subsLoading.value = false
  }
}

function openGrade(sub: Submission) {
  gradingTarget.value = sub
  gradingAssignment.value = assignments.value.find(a => a.name === sub.assignmentName) || null
}

function onGraded() {
  gradingTarget.value = null
  gradingAssignment.value = null
  if (expandedAssignment.value) {
    getSubmissions(expandedAssignment.value).then(s => submissions.value = s)
  }
}

function formatTime(t: string) {
  return new Date(t).toLocaleString('zh-CN')
}

async function handleDelete(a: Assignment) {
  if (!confirm(`确定要删除作业「${a.name}」吗？此操作不可撤销。`)) return
  try {
    await deleteAssignment(a.name)
    await load()
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    error.value = '删除失败: ' + detail
  }
}

defineExpose({ refresh: load })
onMounted(load)
</script>

<template>
  <div class="assignment-list">
    <h3>作业列表</h3>

    <p v-if="loading" class="status">加载中...</p>
    <p v-else-if="error" class="status error">{{ error }}</p>
    <p v-else-if="!assignments.length" class="status">暂无作业</p>

    <div v-else class="cards">
      <div
        v-for="a in assignments"
        :key="a.name"
        class="card"
        :class="{ expanded: expandedAssignment === a.name }"
      >
        <div class="card-header">
          <span class="card-title" @click="toggleSubmissions(a)">{{ a.name }}</span>
          <span class="card-time">{{ formatTime(a.endTime) }} 截止</span>
          <div class="card-actions" @click.stop>
            <button class="btn-edit" @click="emit('edit', a)">编辑</button>
            <button class="btn-delete" @click="handleDelete(a)">删除</button>
          </div>
        </div>

        <div v-if="expandedAssignment === a.name" class="card-body">
          <p v-if="subsLoading">加载提交中...</p>
          <p v-else-if="!submissions.length">暂无提交</p>
          <table v-else>
            <thead>
              <tr>
                <th>学号</th>
                <th>文件名</th>
                <th>提交时间</th>
                <th>分数</th>
                <th>AIGC</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in submissions" :key="s.studentId">
                <td>{{ s.studentId }}</td>
                <td>{{ s.originalFilename }}</td>
                <td>{{ formatTime(s.submissionTime) }}</td>
                <td>{{ s.score != null ? s.score : '-' }}</td>
                <td class="aigc-cell">
                  <span v-if="isAigcDetecting(s)" class="aigc-loading">检测中...</span>
                  <span v-else-if="getAigcScore(s) != null"
                    class="aigc-badge"
                    :class="{
                      high: (getAigcScore(s) ?? 0) >= 80,
                      mid: (getAigcScore(s) ?? 0) >= 50 && (getAigcScore(s) ?? 0) < 80,
                      low: (getAigcScore(s) ?? 0) < 50,
                    }"
                  >
                    {{ getAigcScore(s) }}%
                  </span>
                  <button v-else class="btn-aigc" @click="detectAigc(s)">AI检测</button>
                </td>
                <td class="actions">
                  <button class="btn-preview" @click="previewFile(s.filepath)">预览</button>
                  <button class="btn-download" @click="downloadFile(s.filepath, s.originalFilename)">下载</button>
                  <button class="btn-grade" @click="openGrade(s)">批改</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <GradingPanel
      v-if="gradingTarget"
      :submission="gradingTarget"
      :assignment-requirement="gradingAssignment?.requirement"
      @graded="onGraded"
      @close="gradingTarget = null"
    />
  </div>
</template>

<style scoped>
.assignment-list {
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

.cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card {
  border: 1px solid #eee;
  border-radius: 6px;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fafafa;
}

.card-title {
  font-weight: 600;
  color: #333;
  cursor: pointer;
}

.card-title:hover {
  color: #1890ff;
}

.card-time {
  color: #999;
  font-size: 13px;
  flex: 1;
}

.card-body {
  padding: 16px;
  border-top: 1px solid #eee;
}

.card-body table {
  width: 100%;
  border-collapse: collapse;
}

.card-body th,
.card-body td {
  padding: 6px 10px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}

.card-body th {
  color: #555;
}

.actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.btn-grade {
  padding: 4px 12px;
  background: #faad14;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.btn-grade:hover {
  background: #d48806;
}

.btn-preview,
.btn-download {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  border: none;
}

.btn-preview {
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}

.btn-preview:hover {
  background: #bae7ff;
}

.btn-download {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.btn-download:hover {
  background: #d9f7be;
}

/* AIGC detection */
.aigc-cell {
  text-align: center;
}

.btn-aigc {
  padding: 4px 10px;
  background: #fff7e6;
  color: #d46b08;
  border: 1px solid #ffd591;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.btn-aigc:hover {
  background: #ffe7ba;
}

.aigc-loading {
  font-size: 11px;
  color: #999;
}

.aigc-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.aigc-badge.low {
  background: #f6ffed;
  color: #52c41a;
}

.aigc-badge.mid {
  background: #fff7e6;
  color: #d46b08;
}

.aigc-badge.high {
  background: #fff2f0;
  color: #f5222d;
}

/* 编辑/删除按钮 */
.card-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.btn-edit,
.btn-delete {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  border: none;
}

.btn-edit {
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}

.btn-edit:hover {
  background: #bae7ff;
}

.btn-delete {
  background: #fff2f0;
  color: #f5222d;
  border: 1px solid #ffccc7;
}

.btn-delete:hover {
  background: #ffe7e7;
}
</style>
