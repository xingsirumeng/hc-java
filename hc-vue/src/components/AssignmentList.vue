<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  getAssignmentsByCourse, getSubmissions, downloadFile, previewFile,
  aiAigcDetect, deleteAssignment, batchDeleteAssignments,
  getSimilarities, computeSimilarities,
  batchExtendDeadline,
} from '@/api/teacher'
import type { Assignment, Submission, AssignmentSimilarity } from '@/types/teacher'
import { useToast } from '@/composables/useToast'
import GradingPanel from './GradingPanel.vue'
import BatchAiGrade from './BatchAiGrade.vue'

const { showSuccess, showError, showInfo } = useToast()

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
const batchAiTarget = ref<Assignment | null>(null)

// AIGC 检测结果
const aigcResults = ref<Record<string, { score: number; label: string; loading: boolean }>>({})

// 多选模式
const multiSelectMode = ref(false)
const selectedSubs = ref<Set<string>>(new Set())
const selectAll = ref(false)

// 选中的提交列表
const selectedSubmissionList = computed(() =>
  submissions.value.filter(s => selectedSubs.value.has(`${s.assignmentName}|${s.studentId}`))
)

// 相似度
const similarityTarget = ref<string | null>(null)
const similarities = ref<AssignmentSimilarity[]>([])
const similarityLoading = ref(false)
const similarityComputing = ref(false)

// 批量删除作业
const selectedAssignments = ref<Set<string>>(new Set())
const batchDeleting = ref(false)

// 批量延期
const showBatchExtend = ref(false)
const batchExtendTime = ref('')
const batchExtendReason = ref('')
const batchExtendSubmitting = ref(false)

async function detectAigc(sub: Submission) {
  aigcResults.value = {
    ...aigcResults.value,
    [sub.studentId]: { score: 0, label: '', loading: true },
  }
  try {
    const result = await aiAigcDetect(sub.filepath, sub.assignmentName, sub.studentId)
    aigcResults.value = {
      ...aigcResults.value,
      [sub.studentId]: { score: result.aigc_score, label: result.label, loading: false },
    }
  } catch {
    aigcResults.value = {
      ...aigcResults.value,
      [sub.studentId]: { score: -1, label: '错误', loading: false },
    }
  }
}

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
    selectedSubs.value = new Set()
    selectAll.value = false
    multiSelectMode.value = false
    return
  }

  expandedAssignment.value = a.name
  subsLoading.value = true
  selectedSubs.value = new Set()
  selectAll.value = false
  multiSelectMode.value = false
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

async function onGraded() {
  gradingTarget.value = null
  gradingAssignment.value = null
  if (expandedAssignment.value) {
    const s = await getSubmissions(expandedAssignment.value)
    submissions.value = s
  }
}

function formatTime(t: string) {
  return new Date(t).toLocaleString('zh-CN')
}

async function handleDelete(a: Assignment) {
  if (!confirm(`确定要删除作业「${a.name}」吗？此操作不可撤销。`)) return
  try {
    await deleteAssignment(a.name)
    showSuccess('作业已删除')
    await load()
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    showError('删除失败: ' + detail)
  }
}

// ── 多选模式 ──
function toggleMultiSelect() {
  multiSelectMode.value = !multiSelectMode.value
  if (!multiSelectMode.value) {
    selectedSubs.value = new Set()
    selectAll.value = false
  }
}

function toggleSelectSub(assignmentName: string, studentId: string) {
  const key = `${assignmentName}|${studentId}`
  const s = new Set(selectedSubs.value)
  if (s.has(key)) s.delete(key)
  else s.add(key)
  selectedSubs.value = s
}

function toggleSelectAll() {
  if (selectAll.value) {
    selectedSubs.value = new Set()
    selectAll.value = false
  } else {
    selectedSubs.value = new Set(submissions.value.map(s => `${s.assignmentName}|${s.studentId}`))
    selectAll.value = true
  }
}

// ── 批量延期 ──
async function handleBatchExtend() {
  if (!batchExtendTime.value.trim()) {
    showError('请选择延长后的截止时间')
    return
  }
  const studentIds = multiSelectMode.value && selectedSubs.value.size > 0
    ? submissions.value.filter(s => selectedSubs.value.has(`${s.assignmentName}|${s.studentId}`)).map(s => s.studentId)
    : submissions.value.map(s => s.studentId)

  if (studentIds.length === 0) {
    showError('没有可延期的学生')
    return
  }

  const dt = new Date(batchExtendTime.value)
  const dtStr = dt.getFullYear() + '-' +
    String(dt.getMonth() + 1).padStart(2, '0') + '-' +
    String(dt.getDate()).padStart(2, '0') + ' ' +
    String(dt.getHours()).padStart(2, '0') + ':' +
    String(dt.getMinutes()).padStart(2, '0') + ':00'

  batchExtendSubmitting.value = true
  try {
    const items = studentIds.map(sid => ({
      studentId: sid,
      extendedEndTime: dtStr,
      reason: batchExtendReason.value || undefined,
    }))
    await batchExtendDeadline(expandedAssignment.value!, items)
    showSuccess(`批量延期成功，共处理 ${items.length} 名学生`)
    showBatchExtend.value = false
    batchExtendTime.value = ''
    batchExtendReason.value = ''
  } catch (e: any) {
    showError('批量延期失败: ' + (e?.response?.data?.msg || e?.message || '未知错误'))
  } finally {
    batchExtendSubmitting.value = false
  }
}

// ── 批量删除作业 ──
function toggleSelectAssignment(name: string) {
  const s = new Set(selectedAssignments.value)
  if (s.has(name)) s.delete(name)
  else s.add(name)
  selectedAssignments.value = s
}

async function handleBatchDeleteAssignments() {
  if (selectedAssignments.value.size === 0) {
    showError('请选择要删除的作业')
    return
  }
  if (!confirm(`确定要删除选中的 ${selectedAssignments.value.size} 个作业吗？此操作不可撤销。`)) return

  batchDeleting.value = true
  try {
    await batchDeleteAssignments(Array.from(selectedAssignments.value))
    showSuccess(`批量删除成功，共删除 ${selectedAssignments.value.size} 个作业`)
    selectedAssignments.value = new Set()
    await load()
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    showError('批量删除失败: ' + detail)
  } finally {
    batchDeleting.value = false
  }
}

// ── 相似度 ──
async function openSimilarity(assignmentName: string) {
  similarityTarget.value = assignmentName
  similarityLoading.value = true
  similarityComputing.value = false
  try {
    similarities.value = await getSimilarities(assignmentName)
  } catch (e: any) {
    showError('加载失败: ' + (e?.response?.data?.msg || e?.message || '未知错误'))
  } finally {
    similarityLoading.value = false
  }
}

async function handleComputeSimilarity() {
  if (!similarityTarget.value) return
  similarityComputing.value = true
  try {
    await computeSimilarities(similarityTarget.value)
    similarities.value = await getSimilarities(similarityTarget.value!)
    showSuccess('计算完成')
  } catch (e: any) {
    showError('计算失败: ' + (e?.response?.data?.msg || e?.message || '未知错误'))
  } finally {
    similarityComputing.value = false
  }
}

function closeSimilarity() {
  similarityTarget.value = null
}

// 状态标签
function statusLabel(s: Submission): string {
  if (s.status === 'returned') return '已打回'
  if (s.score != null) return '已批改'
  return '已提交'
}

function statusClass(s: Submission): string {
  if (s.status === 'returned') return 'status-returned'
  if (s.score != null) return 'status-graded'
  return 'status-submitted'
}

defineExpose({ refresh: load })
onMounted(load)
</script>

<template>
  <div class="assignment-list">
    <div class="list-header">
      <h3>作业列表</h3>
      <div class="list-header-actions">
        <button v-if="assignments.length > 0" class="btn-multi-toggle" @click="toggleMultiSelect">
          {{ multiSelectMode ? '退出多选' : '多选模式' }}
        </button>
        <button
          v-if="multiSelectMode && selectedAssignments.size > 0"
          class="btn-batch-delete"
          :disabled="batchDeleting"
          @click="handleBatchDeleteAssignments"
        >
          {{ batchDeleting ? '删除中...' : `删除选中 (${selectedAssignments.size})` }}
        </button>
      </div>
    </div>

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
          <label v-if="multiSelectMode" class="checkbox-label" @click.stop>
            <input
              type="checkbox"
              :checked="selectedAssignments.has(a.name)"
              @change="toggleSelectAssignment(a.name)"
            />
          </label>
          <span class="card-title" @click="toggleSubmissions(a)">{{ a.name }}</span>
          <span class="card-time">{{ formatTime(a.endTime) }} 截止</span>
          <div class="card-actions" @click.stop>
            <button class="btn-sm btn-similarity" @click="openSimilarity(a.name)">相似度</button>
            <button class="btn-edit" @click="emit('edit', a)">编辑</button>
            <button class="btn-delete" @click="handleDelete(a)">删除</button>
          </div>
        </div>

        <div v-if="expandedAssignment === a.name" class="card-body">
          <p v-if="subsLoading">加载提交中...</p>
          <div v-else-if="!submissions.length">
            <p>暂无提交</p>
          </div>
          <div v-else>
            <!-- 批量操作栏（仅多选模式显示） -->
            <div v-if="multiSelectMode" class="batch-bar">
              <label class="checkbox-label">
                <input type="checkbox" :checked="selectAll" @change="toggleSelectAll" />
                全选 ({{ selectedSubs.size }}/{{ submissions.length }})
              </label>
              <button
                v-if="selectedSubs.size > 0"
                class="btn-batch-ai"
                @click="batchAiTarget = a"
              >
                🤖 批量 AI 批改 ({{ selectedSubs.size }})
              </button>
              <button
                v-if="selectedSubs.size > 0"
                class="btn-batch-extend"
                @click="showBatchExtend = !showBatchExtend"
              >
                ⏰ {{ showBatchExtend ? '取消' : '批量延期' }} ({{ selectedSubs.size }})
              </button>
            </div>

            <!-- 批量延期表单 -->
            <div v-if="showBatchExtend" class="batch-extend-form">
              <span class="hint">
                {{ multiSelectMode && selectedSubs.size > 0 ? `已选择 ${selectedSubs.size} 名学生` : '将对全部学生生效' }}
              </span>
              <input v-model="batchExtendTime" type="datetime-local" class="extend-input" />
              <input v-model="batchExtendReason" type="text" placeholder="原因（可选）" class="extend-input" />
              <button class="btn-extend-confirm" :disabled="batchExtendSubmitting" @click="handleBatchExtend">
                {{ batchExtendSubmitting ? '处理中...' : '确认延期' }}
              </button>
            </div>

            <table>
              <thead>
                <tr>
                  <th v-if="multiSelectMode" style="width:36px"></th>
                  <th>学号</th>
                  <th>文件名</th>
                  <th>提交时间</th>
                  <th>状态</th>
                  <th>分数</th>
                  <th>AIGC</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in submissions" :key="s.studentId" :class="{ 'row-returned': s.status === 'returned' }">
                  <td v-if="multiSelectMode">
                    <input
                      type="checkbox"
                      :checked="selectedSubs.has(`${s.assignmentName}|${s.studentId}`)"
                      @change="toggleSelectSub(s.assignmentName, s.studentId)"
                    />
                  </td>
                  <td>{{ s.studentId }}</td>
                  <td>
                    {{ s.originalFilename }}
                    <span v-if="s.status === 'returned' && s.returnReason" class="return-reason" :title="s.returnReason">
                      ⚠ {{ s.returnReason }}
                    </span>
                  </td>
                  <td>{{ formatTime(s.submissionTime) }}</td>
                  <td>
                    <span class="status-badge" :class="statusClass(s)">{{ statusLabel(s) }}</span>
                  </td>
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
                    <button class="btn-download" @click="downloadFile(s.filepath, s.originalFilename)">下载</button>
                    <button class="btn-grade" @click="openGrade(s)">批改</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- 批改面板 -->
    <GradingPanel
      v-if="gradingTarget"
      :submission="gradingTarget"
      :assignment-requirement="gradingAssignment?.requirement"
      :all-submissions="submissions"
      @graded="onGraded"
      @navigate="gradingTarget = $event"
      @close="gradingTarget = null"
    />

    <!-- 批量 AI 批改 -->
    <BatchAiGrade
      v-if="batchAiTarget"
      :submissions="selectedSubmissionList"
      :assignment-name="batchAiTarget.name"
      :assignment-requirement="batchAiTarget.requirement"
      @graded="batchAiTarget = null; onGraded()"
      @close="batchAiTarget = null"
    />

    <!-- 相似度弹窗 -->
    <div v-if="similarityTarget" class="modal-overlay" @click.self="closeSimilarity">
      <div class="modal similarity-modal">
        <div class="modal-header">
          <h2>作业相似度 — {{ similarityTarget }}</h2>
          <button class="btn-close-x" @click="closeSimilarity">✕</button>
        </div>
        <div class="similarity-actions">
          <button
            class="btn-compute"
            :disabled="similarityComputing"
            @click="handleComputeSimilarity"
          >
            {{ similarityComputing ? '⏳ 计算中...' : '🔄 重新计算' }}
          </button>
        </div>
        <p v-if="similarityComputing" style="text-align:center;color:#999;padding:20px;">计算中，请稍候...</p>
        <p v-else-if="similarityLoading">加载中...</p>
        <p v-else-if="!similarities.length" style="color:#999;text-align:center;padding:20px;">
          暂无数据，点击上方按钮开始计算
        </p>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>学生 A</th>
              <th>学生 B</th>
              <th>相似度</th>
              <th>详情</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in similarities" :key="item.id"
              :class="{
                'high-sim': (item.similarityScore ?? 0) >= 80,
                'mid-sim': (item.similarityScore ?? 0) >= 50 && (item.similarityScore ?? 0) < 80,
              }"
            >
              <td>{{ item.studentId1 }}</td>
              <td>{{ item.studentId2 }}</td>
              <td>
                <template v-if="(item.similarityScore ?? 0) < 0">
                  <span class="sim-score na">无法计算</span>
                </template>
                <template v-else>
                  <span class="sim-score" :class="{
                    high: (item.similarityScore ?? 0) >= 80,
                    mid: (item.similarityScore ?? 0) >= 50 && (item.similarityScore ?? 0) < 80,
                    low: (item.similarityScore ?? 0) < 50,
                  }">{{ item.similarityScore ?? '-' }}%</span>
                </template>
              </td>
              <td class="sim-detail">{{ item.detail || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.assignment-list {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.list-header h3 { margin: 0; font-size: 16px; color: #333; }

.list-header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.status {
  font-size: 14px;
  color: #999;
}

.status.error { color: #f5222d; }
.status.success { color: #52c41a; }

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
  flex: 1;
}

.card-title:hover { color: #1890ff; }

.card-time {
  color: #999;
  font-size: 13px;
  flex-shrink: 0;
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
  padding: 6px 8px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}

.card-body th { color: #555; }

/* 多选切换 */
.btn-multi-toggle {
  padding: 4px 12px;
  background: #fff;
  color: #4a90d9;
  border: 1px solid #4a90d9;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.btn-multi-toggle:hover { background: #f0f7ff; }

/* 批量操作栏 */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 4px;
}

.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #555;
  cursor: pointer;
}

.btn-batch-grade {
  padding: 4px 12px;
  background: #4a90d9;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.btn-batch-grade:hover:not(:disabled) { background: #357abd; }
.btn-batch-grade:disabled { background: #a0c4e8; cursor: not-allowed; }

.btn-batch-ai {
  padding: 4px 14px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  margin-left: auto;
}

.btn-batch-ai:hover { opacity: 0.9; }

.btn-batch-extend {
  padding: 4px 14px;
  background: #e6fffb;
  color: #13c2c2;
  border: 1px solid #87e8de;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.btn-batch-extend:hover { background: #b5f5ec; }

.batch-extend-form {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #e6fffb;
  border: 1px solid #87e8de;
  border-radius: 4px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.batch-extend-form .hint {
  font-size: 12px;
  color: #666;
  flex-shrink: 0;
}

.batch-extend-form .extend-input {
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 12px;
}

.btn-extend-confirm {
  padding: 4px 12px;
  background: #13c2c2;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.btn-extend-confirm:hover:not(:disabled) { background: #08979c; }
.btn-extend-confirm:disabled { background: #87e8de; cursor: not-allowed; }

.btn-batch-delete {
  padding: 4px 12px;
  background: #ff4d4f;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.btn-batch-delete:hover:not(:disabled) { background: #d9363e; }
.btn-batch-delete:disabled { background: #ff9999; cursor: not-allowed; }

/* 状态标签 */
.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.status-submitted { background: #e6f7ff; color: #1890ff; }
.status-graded { background: #f6ffed; color: #52c41a; }
.status-returned { background: #fff2f0; color: #f5222d; }

.return-reason {
  display: block;
  font-size: 11px;
  color: #f5222d;
  cursor: help;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-returned { background: #fff7f7; }

.actions {
  display: flex;
  gap: 4px;
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

.btn-grade:hover { background: #d48806; }

.btn-download {
  padding: 4px 10px;
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.btn-download:hover { background: #d9f7be; }

/* AIGC */
.aigc-cell { text-align: center; }

.btn-aigc {
  padding: 4px 10px;
  background: #fff7e6;
  color: #d46b08;
  border: 1px solid #ffd591;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.btn-aigc:hover { background: #ffe7ba; }

.aigc-loading { font-size: 11px; color: #999; }

.aigc-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.aigc-badge.low { background: #f6ffed; color: #52c41a; }
.aigc-badge.mid { background: #fff7e6; color: #d46b08; }
.aigc-badge.high { background: #fff2f0; color: #f5222d; }

/* 编辑/删除按钮 */
.card-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  flex-wrap: wrap;
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

.btn-edit:hover { background: #bae7ff; }

.btn-delete {
  background: #fff2f0;
  color: #f5222d;
  border: 1px solid #ffccc7;
}

.btn-delete:hover { background: #ffe7e7; }

.btn-sm {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  border: none;
}

.btn-similarity {
  background: #f9f0ff;
  color: #722ed1;
  border: 1px solid #d3adf7;
}

.btn-similarity:hover { background: #efdbff; }

/* 相似度弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  max-width: 90vw;
  max-height: 85vh;
  overflow-y: auto;
}

.similarity-modal { width: 700px; }

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.modal-header h2 { margin: 0; font-size: 18px; }

.btn-close-x {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
  padding: 0 4px;
}

.btn-close-x:hover { color: #333; }

.similarity-actions { margin-bottom: 16px; display: flex; gap: 8px; align-items: center; }

.btn-stop {
  padding: 8px 16px;
  background: #ff4d4f;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
}

.btn-stop:hover { background: #d9363e; }

.btn-compute {
  padding: 8px 20px;
  background: #722ed1;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.btn-compute:hover:not(:disabled) { background: #531dab; }
.btn-compute:disabled { background: #b37feb; cursor: not-allowed; }

.progress-bar-wrap {
  width: 100%; height: 6px; background: #f0f0f0; border-radius: 3px;
  margin-bottom: 12px; overflow: hidden;
}
.progress-bar-fill {
  height: 100%; background: linear-gradient(90deg, #667eea, #764ba2);
  border-radius: 3px; transition: width 0.5s ease;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}

.data-table th {
  background: #fafafa;
  color: #555;
  font-weight: 600;
}

.sim-score { font-weight: 600; }
.sim-score.high { color: #f5222d; }
.sim-score.mid { color: #fa8c16; }
.sim-score.low { color: #52c41a; }
.sim-score.na { color: #999; font-style: italic; }

.high-sim { background: #fff7f7; }
.mid-sim { background: #fffdf7; }

.sim-detail {
  font-size: 12px;
  color: #666;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
