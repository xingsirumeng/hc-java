<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  getStudentProfile,
  updateStudentProfile,
  getStudentCourses,
  getStudentAssignments,
  getStudentAssignmentDetail,
  submitStudentAssignment,
  getStudentSubmissions,
  uploadFile,
  downloadFile,
  previewFile,
} from '@/api/teacher'
import type { Course, StudentAssignmentDTO, Submission } from '@/types/teacher'

const router = useRouter()

// ── 状态 ──
const activeTab = ref<'assignments' | 'submissions' | 'profile'>('assignments')
const studentName = ref(localStorage.getItem('studentName') || '')
const studentId = ref(localStorage.getItem('studentId') || '')

// 课程列表
const courses = ref<Course[]>([])
const coursesLoading = ref(false)

// 选中的课程（null = 显示课程列表）
const selectedCourse = ref<Course | null>(null)

// 作业列表（全部）
const allAssignments = ref<StudentAssignmentDTO[]>([])
const assignmentsLoading = ref(false)
const assignmentsError = ref('')

// 当前课程的作业
const courseAssignments = computed(() =>
  selectedCourse.value
    ? allAssignments.value.filter(a => a.courseId === selectedCourse.value!.courseId)
    : []
)

const overdueAssignments = computed(() =>
  courseAssignments.value.filter(a => !a.submitted && new Date(a.endTime) < new Date())
)
const upcomingAssignments = computed(() =>
  courseAssignments.value.filter(a => !a.submitted && new Date(a.endTime) >= new Date())
)
const submittedAssignments = computed(() =>
  courseAssignments.value.filter(a => a.submitted)
)

// 作业详情（弹窗）
const selectedAssignment = ref<StudentAssignmentDTO | null>(null)
const detailLoading = ref(false)
const detailError = ref('')

// 提交相关
const selectedFile = ref<File | null>(null)
const submitting = ref(false)
const submitMsg = ref('')
const submitError = ref(false)

// 提交记录
const submissions = ref<Submission[]>([])
const submissionsLoading = ref(false)
const submissionsError = ref('')

// 按当前课程过滤提交记录
const filteredSubmissions = computed(() => {
  if (!selectedCourse.value) return submissions.value
  const courseAssignmentNames = new Set(
    allAssignments.value
      .filter(a => a.courseId === selectedCourse.value!.courseId)
      .map(a => a.assignmentName)
  )
  return submissions.value.filter(s => courseAssignmentNames.has(s.assignmentName))
})

// ── 课程成绩统计 ──
interface ScorePoint {
  assignmentName: string
  startTime: Date
  score: number
}

const scorePoints = computed<ScorePoint[]>(() => {
  return filteredSubmissions.value
    .filter(s => s.score != null)
    .map(s => {
      const asgn = allAssignments.value.find(a => a.assignmentName === s.assignmentName)
      return {
        assignmentName: s.assignmentName,
        startTime: asgn ? new Date(asgn.startTime) : new Date(0),
        score: s.score as number,
      }
    })
    .sort((a, b) => a.startTime.getTime() - b.startTime.getTime())
})

const avgScore = computed(() => {
  if (!scorePoints.value.length) return null
  const sum = scorePoints.value.reduce((a, b) => a + b.score, 0)
  return Math.round((sum / scorePoints.value.length) * 10) / 10
})

const singleScore = computed(() =>
  scorePoints.value.length === 1 ? scorePoints.value[0] : null
)

// SVG 折线图参数
const chartW = 600
const chartH = 200
const chartPad = { top: 20, right: 20, bottom: 36, left: 40 }
const chartInnerW = chartW - chartPad.left - chartPad.right
const chartInnerH = chartH - chartPad.top - chartPad.bottom

const linePoints = computed(() => {
  if (scorePoints.value.length < 2) return ''
  const pts = scorePoints.value
  const t0 = pts[0]!.startTime.getTime()
  const tMax = pts[pts.length - 1]!.startTime.getTime()
  const tRange = tMax - t0 || 1
  const x = (t: number) => chartPad.left + ((t - t0) / tRange) * chartInnerW
  const y = (s: number) => chartPad.top + chartInnerH - (s / 100) * chartInnerH
  return pts.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(p.startTime.getTime()).toFixed(1)},${y(p.score).toFixed(1)}`).join(' ')
})

const pointCircles = computed(() => {
  const pts = scorePoints.value
  if (pts.length < 2) return []
  const t0 = pts[0]!.startTime.getTime()
  const tMax = pts[pts.length - 1]!.startTime.getTime()
  const tRange = tMax - t0 || 1
  return pts.map(p => ({
    cx: (chartPad.left + ((p.startTime.getTime() - t0) / tRange) * chartInnerW).toFixed(1),
    cy: (chartPad.top + chartInnerH - (p.score / 100) * chartInnerH).toFixed(1),
    name: p.assignmentName,
    score: p.score,
  }))
})

// 评语展开状态
const expandedComments = ref<Set<string>>(new Set())

function toggleComment(key: string) {
  const s = new Set(expandedComments.value)
  if (s.has(key)) s.delete(key)
  else s.add(key)
  expandedComments.value = s
}

function isCommentExpanded(key: string): boolean {
  return expandedComments.value.has(key)
}

// 个人资料
const profileForm = ref({ name: '', password: '', confirmPassword: '' })
const profileSaving = ref(false)
const profileMsg = ref('')
const profileError = ref(false)

// ── 加载课程 ──
async function loadCourses() {
  coursesLoading.value = true
  try {
    courses.value = await getStudentCourses()
  } catch (_) {
    /* ignore */
  } finally {
    coursesLoading.value = false
  }
}

// ── 加载全部作业 ──
async function loadAllAssignments() {
  assignmentsLoading.value = true
  assignmentsError.value = ''
  try {
    allAssignments.value = await getStudentAssignments()
  } catch (e: any) {
    assignmentsError.value = e?.response?.data?.msg || e?.message || '加载失败'
  } finally {
    assignmentsLoading.value = false
  }
}

// ── 进入课程，查看作业 ──
function enterCourse(course: Course) {
  selectedCourse.value = course
}

// ── 返回课程列表 ──
function backToCourses() {
  selectedCourse.value = null
}

// ── 查看作业详情 ──
async function viewAssignment(assignmentName: string) {
  detailLoading.value = true
  detailError.value = ''
  selectedAssignment.value = null
  try {
    selectedAssignment.value = await getStudentAssignmentDetail(assignmentName)
  } catch (e: any) {
    detailError.value = e?.response?.data?.msg || e?.message || '加载失败'
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  selectedAssignment.value = null
  selectedFile.value = null
  submitMsg.value = ''
}

// ── 文件选择 ──
function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files?.length) {
    selectedFile.value = input.files[0] as File
  }
}

// ── 提交作业 ──
async function handleSubmit() {
  if (!selectedAssignment.value || !selectedFile.value) {
    submitMsg.value = '请选择文件'
    submitError.value = true
    return
  }

  submitting.value = true
  submitMsg.value = ''
  submitError.value = false

  try {
    const uploadResult = await uploadFile(selectedFile.value)
    await submitStudentAssignment(
      selectedAssignment.value.assignmentName,
      uploadResult.filepath,
      uploadResult.originalFilename,
    )

    submitMsg.value = '✅ 提交成功'
    selectedFile.value = null
    const fileInput = document.getElementById('fileInput') as HTMLInputElement
    if (fileInput) fileInput.value = ''

    await loadAllAssignments()
    await viewAssignment(selectedAssignment.value.assignmentName)
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    submitMsg.value = '提交失败: ' + detail
    submitError.value = true
  } finally {
    submitting.value = false
  }
}

// ── 加载提交记录 ──
async function loadSubmissions() {
  submissionsLoading.value = true
  submissionsError.value = ''
  try {
    submissions.value = await getStudentSubmissions()
  } catch (e: any) {
    submissionsError.value = e?.response?.data?.msg || e?.message || '加载失败'
  } finally {
    submissionsLoading.value = false
  }
}

// ── 加载个人资料 ──
async function loadProfile() {
  try {
    const profile = await getStudentProfile()
    profileForm.value.name = profile.name
    profileForm.value.password = ''
    profileForm.value.confirmPassword = ''
  } catch (_) {
    /* ignore */
  }
}

// ── 保存个人资料 ──
async function handleSaveProfile() {
  profileMsg.value = ''
  profileError.value = false

  if (profileForm.value.password && profileForm.value.password !== profileForm.value.confirmPassword) {
    profileMsg.value = '两次输入的密码不一致'
    profileError.value = true
    return
  }

  profileSaving.value = true
  try {
    const data: { name?: string; password?: string } = {}
    if (profileForm.value.name && profileForm.value.name !== studentName.value) {
      data.name = profileForm.value.name
    }
    if (profileForm.value.password) {
      data.password = profileForm.value.password
    }
    if (!data.name && !data.password) {
      profileMsg.value = '没有需要修改的内容'
      profileError.value = true
      return
    }

    await updateStudentProfile(data)
    if (data.name) {
      studentName.value = data.name
      localStorage.setItem('studentName', data.name)
    }
    profileForm.value.password = ''
    profileForm.value.confirmPassword = ''
    profileMsg.value = '✅ 资料更新成功'
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    profileMsg.value = '保存失败: ' + detail
    profileError.value = true
  } finally {
    profileSaving.value = false
  }
}

// ── 退出 ──
function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userRole')
  localStorage.removeItem('studentName')
  localStorage.removeItem('studentId')
  router.push('/login/student')
}

// ── 格式化日期 ──
function fmtDate(d: string | null) {
  if (!d) return '-'
  return new Date(d).toLocaleString('zh-CN')
}

function deadlineColor(endTime: string) {
  const now = new Date()
  const end = new Date(endTime)
  const diff = end.getTime() - now.getTime()
  if (diff < 0) return '#f5222d'
  if (diff < 24 * 3600 * 1000) return '#fa8c16'
  return '#52c41a'
}

// ── 初始化 ──
onMounted(async () => {
  await Promise.all([loadCourses(), loadAllAssignments()])
})
</script>

<template>
  <div class="student-app">
    <!-- 顶栏 -->
    <header class="header">
      <h1>🎓 学生端</h1>
      <div class="header-right">
        <span class="student-info">{{ studentId }} {{ studentName }}</span>
        <button class="btn-logout" @click="logout">退出</button>
      </div>
    </header>

    <!-- Tab 切换 -->
    <nav class="tabs">
      <button
        :class="{ active: activeTab === 'assignments' }"
        @click="activeTab = 'assignments'"
      >
        {{ selectedCourse ? selectedCourse.courseName : '课程' }}
        <span v-if="overdueAssignments.length && selectedCourse" class="badge danger">{{ overdueAssignments.length }}</span>
      </button>
      <button
        :class="{ active: activeTab === 'submissions' }"
        @click="activeTab = 'submissions'; loadSubmissions()"
      >
        {{ selectedCourse ? selectedCourse.courseName + ' 提交' : '提交记录' }}
      </button>
      <button
        :class="{ active: activeTab === 'profile' }"
        @click="activeTab = 'profile'; loadProfile()"
      >
        个人资料
      </button>
    </nav>

    <!-- ────────── 课程 / 作业 Tab ────────── -->
    <section v-if="activeTab === 'assignments'" class="tab-content">

      <!-- ===== 课程列表视图 ===== -->
      <template v-if="!selectedCourse">
        <h3 style="margin: 0 0 16px; color: #333;">我的课程</h3>
        <p v-if="coursesLoading">加载中...</p>
        <p v-else-if="!courses.length" class="empty">暂无课程</p>
        <div v-else class="course-grid">
          <div
            v-for="c in courses"
            :key="c.courseId"
            class="course-card"
            @click="enterCourse(c)"
          >
            <h4>{{ c.courseName }}</h4>
            <p class="course-id">{{ c.courseId }}</p>
            <p class="course-credit">学分: {{ c.credit }}</p>
            <p class="course-teacher">教师: {{ c.teacherId }}</p>
          </div>
        </div>
      </template>

      <!-- ===== 课程详情视图（作业列表） ===== -->
      <template v-else>
        <!-- 返回按钮 -->
        <button class="btn-back" @click="backToCourses">← 返回课程列表</button>

        <h2 class="course-title">{{ selectedCourse.courseName }}</h2>
        <p class="course-subtitle">{{ selectedCourse.courseId }} · {{ selectedCourse.teacherId }} · {{ selectedCourse.credit }} 学分</p>

        <p v-if="assignmentsLoading">加载中...</p>
        <p v-else-if="assignmentsError" class="error">{{ assignmentsError }}</p>

        <template v-else>
          <!-- 统计 -->
          <div class="stats">
            <div class="stat-item">
              <span class="stat-num">{{ upcomingAssignments.length }}</span>
              <span class="stat-label">待提交</span>
            </div>
            <div class="stat-item">
              <span class="stat-num overdue">{{ overdueAssignments.length }}</span>
              <span class="stat-label">已逾期</span>
            </div>
            <div class="stat-item">
              <span class="stat-num done">{{ submittedAssignments.length }}</span>
              <span class="stat-label">已提交</span>
            </div>
          </div>

          <!-- 成绩统计 -->
          <div v-if="avgScore != null" class="score-stats-card">
            <div class="avg-score-block">
              <span class="avg-label">📊 课程平均分</span>
              <span class="avg-value">{{ avgScore }}</span>
              <span class="avg-sub">共 {{ scorePoints.length }} 次作业</span>
            </div>
            <!-- SVG 折线图 -->
            <div v-if="scorePoints.length >= 2" class="line-chart-wrap">
              <svg :viewBox="'0 0 ' + chartW + ' ' + chartH" class="line-chart-svg">
                <!-- Y轴参考线 -->
                <line v-for="yval in [0,20,40,60,80,100]" :key="'y' + yval"
                  :x1="chartPad.left" :x2="chartW - chartPad.right"
                  :y1="chartPad.top + chartInnerH - (yval/100)*chartInnerH"
                  :y2="chartPad.top + chartInnerH - (yval/100)*chartInnerH"
                  stroke="#f0f0f0" stroke-width="1" />
                <text v-for="yval in [0,20,40,60,80,100]" :key="'yt' + yval"
                  :x="chartPad.left - 6" text-anchor="end"
                  :y="chartPad.top + chartInnerH - (yval/100)*chartInnerH + 4"
                  font-size="12" fill="#999">{{ yval }}</text>
                <!-- 折线 -->
                <path v-if="linePoints" :d="linePoints" fill="none" stroke="#1890ff" stroke-width="2.5" stroke-linejoin="round" />
                <!-- 数据点 + 标签 -->
                <g v-for="(c, i) in pointCircles" :key="c.name">
                  <circle :cx="c.cx" :cy="c.cy" r="5" fill="#1890ff" stroke="#fff" stroke-width="2" />
                  <text :x="c.cx" :y="Number(c.cy) - 12" text-anchor="middle" font-size="11" fill="#333" font-weight="600">{{ c.score }}</text>
                  <text :x="c.cx" :y="chartH - 4" text-anchor="middle" font-size="10" fill="#999">{{ c.name }}</text>
                </g>
              </svg>
            </div>
            <div v-else-if="singleScore" class="single-score">
              <span class="single-val">{{ singleScore.score }}分</span>
              <span class="single-name">{{ singleScore.assignmentName }}</span>
            </div>
          </div>

          <!-- 未提交 -->
          <h3 v-if="upcomingAssignments.length + overdueAssignments.length" class="section-label">⏳ 待提交</h3>

          <div class="assign-list">
            <div
              v-for="a in [...overdueAssignments, ...upcomingAssignments]"
              :key="a.assignmentName"
              class="assign-card"
              :class="{ overdue: new Date(a.endTime) < new Date() }"
              @click="viewAssignment(a.assignmentName)"
            >
              <div class="assign-main">
                <h4>{{ a.assignmentName }}</h4>
                <p class="assign-meta">{{ a.courseName }}</p>
                <p class="assign-deadline" :style="{ color: deadlineColor(a.endTime) }">
                  ⏰ 截止: {{ fmtDate(a.endTime) }}
                </p>
              </div>
              <div class="assign-status overdue-tag" v-if="new Date(a.endTime) < new Date()">已逾期</div>
              <div class="assign-status pending-tag" v-else>待提交</div>
            </div>
          </div>

          <!-- 已提交 -->
          <h3 v-if="submittedAssignments.length" class="section-label">✅ 已提交</h3>

          <div class="assign-list">
            <div
              v-for="a in submittedAssignments"
              :key="a.assignmentName"
              class="assign-card submitted"
              @click="viewAssignment(a.assignmentName)"
            >
              <div class="assign-main">
                <h4>{{ a.assignmentName }}</h4>
                <p class="assign-meta">{{ a.courseName }}</p>
                <p class="assign-submitted">
                  提交于 {{ fmtDate(a.submissionTime) }}
                  <span v-if="a.score != null" class="score-badge">得分: {{ a.score }}</span>
                </p>
              </div>
              <div class="assign-status done-tag">
                {{ a.score != null ? '已批改' : '已提交' }}
              </div>
            </div>
          </div>

          <p v-if="!courseAssignments.length" class="empty">该课程暂无作业</p>
        </template>
      </template>

      <!-- 作业详情弹窗 -->
      <div v-if="selectedAssignment" class="modal-overlay" @click.self="closeDetail">
        <div class="modal">
          <button class="modal-close" @click="closeDetail">✕</button>

          <div v-if="detailLoading">加载中...</div>
          <div v-else-if="detailError" class="error">{{ detailError }}</div>

          <template v-else>
            <h2>{{ selectedAssignment.assignmentName }}</h2>
            <div class="detail-meta">
              <p><strong>课程:</strong> {{ selectedAssignment.courseName }}</p>
              <p><strong>开始:</strong> {{ fmtDate(selectedAssignment.startTime) }}</p>
              <p><strong>截止:</strong>
                <span :style="{ color: deadlineColor(selectedAssignment.endTime) }">
                  {{ fmtDate(selectedAssignment.endTime) }}
                </span>
              </p>
            </div>

            <div v-if="selectedAssignment.requirement" class="detail-req">
              <h4>作业要求</h4>
              <pre>{{ selectedAssignment.requirement }}</pre>
            </div>

            <!-- 已提交 -->
            <div v-if="selectedAssignment.submitted" class="detail-submitted">
              <h4>📤 你的提交</h4>
              <p>提交时间: {{ fmtDate(selectedAssignment.submissionTime) }}</p>
              <p v-if="selectedAssignment.originalFilename">文件: {{ selectedAssignment.originalFilename }}</p>
              <div class="detail-actions">
                <button
                  v-if="selectedAssignment.filepath"
                  class="btn-sm"
                  @click="downloadFile(selectedAssignment.filepath!, selectedAssignment.originalFilename || 'file')"
                >📥 下载</button>
                <button
                  v-if="selectedAssignment.filepath"
                  class="btn-sm btn-outline"
                  @click="previewFile(selectedAssignment.filepath!)"
                >👁 预览</button>
              </div>
              <div v-if="selectedAssignment.score != null" class="grade-box">
                <h4>📝 成绩</h4>
                <p class="score">{{ selectedAssignment.score }} 分</p>
                <p
                  v-if="selectedAssignment.comment"
                  class="comment"
                  :class="{ truncated: !isCommentExpanded('detail') }"
                  :title="isCommentExpanded('detail') ? '' : '点击查看完整评语'"
                  @click="toggleComment('detail')"
                >「{{ selectedAssignment.comment }}」</p>
                <p v-if="selectedAssignment.aigcScore != null" class="aigc">AIGC 检测分: {{ selectedAssignment.aigcScore }}</p>
                <div v-if="selectedAssignment.annotatedFilepath" class="annotated-section">
                  <h4>🖊 教师批注</h4>
                  <img
                    :src="'/api/file/preview?filepath=' + encodeURIComponent(selectedAssignment.annotatedFilepath)"
                    class="annotated-img"
                    :alt="'教师批注'"
                    @click="previewFile(selectedAssignment.annotatedFilepath!)"
                    title="点击放大查看"
                  />
                  <button class="btn-sm btn-outline" style="margin-top: 8px;" @click="previewFile(selectedAssignment.annotatedFilepath!)">
                    🔍 查看批注大图
                  </button>
                </div>
              </div>
            </div>

            <!-- 提交表单 -->
            <div class="submit-section">
              <h4>{{ selectedAssignment.submitted ? '📎 重新提交' : '📎 提交作业' }}</h4>
              <input id="fileInput" type="file" @change="onFileChange" />
              <button class="btn-submit" :disabled="submitting" @click="handleSubmit">
                {{ submitting ? '提交中...' : '提交' }}
              </button>
              <p v-if="submitMsg" :class="submitError ? 'error' : 'success'">{{ submitMsg }}</p>
            </div>
          </template>
        </div>
      </div>
    </section>

    <!-- ────────── 提交记录 Tab ────────── -->
    <section v-if="activeTab === 'submissions'" class="tab-content">
      <p v-if="submissionsLoading">加载中...</p>
      <p v-else-if="submissionsError" class="error">{{ submissionsError }}</p>

      <template v-else>
        <p v-if="!selectedCourse" class="empty">请先在「课程」中选择一门课程</p>
        <div v-else-if="!filteredSubmissions.length" class="empty">该课程暂无提交记录</div>

        <div v-for="s in filteredSubmissions" :key="s.assignmentName" class="submission-card">
          <div class="sub-main">
            <h4>{{ s.assignmentName }}</h4>
            <p>提交时间: {{ fmtDate(s.submissionTime) }}</p>
            <p v-if="s.originalFilename">文件: {{ s.originalFilename }}</p>
            <div class="sub-actions">
              <button class="btn-sm" @click="downloadFile(s.filepath, s.originalFilename || 'file')">📥 下载</button>
              <button class="btn-sm btn-outline" @click="previewFile(s.filepath)">👁 预览</button>
            </div>
          </div>
          <div class="sub-grade">
            <template v-if="s.score != null">
              <span class="score">{{ s.score }} 分</span>
              <p
                v-if="s.comment"
                class="comment"
                :class="{ truncated: !isCommentExpanded(s.assignmentName) }"
                :title="isCommentExpanded(s.assignmentName) ? '' : '点击查看完整评语'"
                @click="toggleComment(s.assignmentName)"
              >「{{ s.comment }}」</p>
              <p v-if="s.aigcScore != null" class="aigc">AIGC: {{ s.aigcScore }}</p>
              <button
                v-if="s.annotatedFilepath"
                class="btn-sm btn-outline"
                style="margin-top: 6px;"
                @click="previewFile(s.annotatedFilepath!)"
              >🖊 查看批注</button>
            </template>
            <span v-else class="pending">待批改</span>
          </div>
        </div>
      </template>
    </section>

    <!-- ────────── 个人资料 Tab ────────── -->
    <section v-if="activeTab === 'profile'" class="tab-content">
      <div class="profile-card">
        <h3>个人信息</h3>
        <div class="form-group">
          <label>学号</label>
          <input :value="studentId" type="text" disabled />
        </div>
        <div class="form-group">
          <label>姓名</label>
          <input v-model="profileForm.name" type="text" placeholder="姓名" />
        </div>
        <div class="form-group">
          <label>新密码（留空则不修改）</label>
          <input v-model="profileForm.password" type="password" placeholder="新密码" />
        </div>
        <div class="form-group">
          <label>确认密码</label>
          <input v-model="profileForm.confirmPassword" type="password" placeholder="再次输入新密码" />
        </div>
        <button :disabled="profileSaving" @click="handleSaveProfile">
          {{ profileSaving ? '保存中...' : '保存修改' }}
        </button>
        <p v-if="profileMsg" :class="profileError ? 'error' : 'success'">{{ profileMsg }}</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.student-app {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 16px 40px;
}

/* 顶栏 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 0 16px;
}
.header h1 { margin: 0; font-size: 22px; color: #333; }
.header-right { display: flex; align-items: center; gap: 16px; }
.student-info { color: #888; font-size: 14px; }
.btn-logout {
  padding: 6px 16px; background: #f5f5f5; border: 1px solid #ddd;
  border-radius: 4px; cursor: pointer; font-size: 13px; color: #666;
}
.btn-logout:hover { background: #e8e8e8; }

/* Tab */
.tabs {
  display: flex; border-bottom: 2px solid #f0f0f0; margin-bottom: 24px;
}
.tabs button {
  padding: 10px 24px; border: none; background: none; font-size: 15px;
  color: #888; cursor: pointer; border-bottom: 2px solid transparent;
  margin-bottom: -2px; transition: color 0.2s, border-color 0.2s; position: relative;
}
.tabs button.active { color: #52c41a; border-bottom-color: #52c41a; font-weight: 500; }
.tabs button:hover { color: #52c41a; }
.badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; margin-left: 6px; border-radius: 9px;
  font-size: 11px; color: #fff; padding: 0 5px;
}
.badge.danger { background: #f5222d; }

/* 课程卡片 */
.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.course-card {
  background: #fff;
  border-radius: 10px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  border-left: 4px solid #4a90d9;
}
.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}
.course-card h4 { margin: 0 0 8px; font-size: 17px; color: #333; }
.course-id { margin: 0 0 4px; font-size: 13px; color: #999; }
.course-credit { margin: 0 0 4px; font-size: 13px; color: #666; }
.course-teacher { margin: 0; font-size: 13px; color: #888; }

/* 返回按钮 */
.btn-back {
  background: none; border: none; color: #4a90d9; font-size: 14px;
  cursor: pointer; padding: 0; margin-bottom: 12px;
}
.btn-back:hover { color: #357abd; }

/* 课程标题 */
.course-title { margin: 0 0 4px; font-size: 20px; color: #333; }
.course-subtitle { margin: 0 0 20px; font-size: 13px; color: #999; }

/* 统计 */
.stats { display: flex; gap: 16px; margin-bottom: 24px; }
.stat-item {
  flex: 1; background: #fff; border-radius: 8px; padding: 20px;
  text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.stat-num { display: block; font-size: 28px; font-weight: 700; color: #4a90d9; }
.stat-num.overdue { color: #f5222d; }
.stat-num.done { color: #52c41a; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }

/* 作业列表 */
.section-label { margin: 0 0 12px; font-size: 16px; color: #333; }
.assign-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 24px; }
.assign-card {
  background: #fff; border-radius: 8px; padding: 16px 20px; display: flex;
  justify-content: space-between; align-items: center;
  box-shadow: 0 1px 6px rgba(0,0,0,0.06); cursor: pointer;
  transition: box-shadow 0.15s; border-left: 4px solid #52c41a;
}
.assign-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.assign-card.overdue { border-left-color: #f5222d; }
.assign-card.submitted { border-left-color: #bbb; }
.assign-main h4 { margin: 0; font-size: 15px; color: #333; }
.assign-meta { margin: 4px 0; font-size: 13px; color: #888; }
.assign-deadline { margin: 0; font-size: 13px; font-weight: 500; }
.assign-submitted { margin: 4px 0; font-size: 13px; color: #888; }
.score-badge {
  margin-left: 8px; background: #e6f7ff; color: #1890ff;
  padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 500;
}
.assign-status { font-size: 12px; padding: 4px 10px; border-radius: 4px; font-weight: 500; white-space: nowrap; }
.overdue-tag { background: #fff2f0; color: #f5222d; }
.pending-tag { background: #fff7e6; color: #fa8c16; }
.done-tag { background: #f6ffed; color: #52c41a; }

/* 弹窗 */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45);
  display: flex; align-items: flex-start; justify-content: center; padding-top: 60px; z-index: 100;
}
.modal {
  background: #fff; border-radius: 12px; padding: 32px; width: 100%;
  max-width: 600px; max-height: 80vh; overflow-y: auto; position: relative;
}
.modal-close {
  position: absolute; top: 12px; right: 16px; background: none;
  border: none; font-size: 22px; cursor: pointer; color: #999;
}
.modal-close:hover { color: #333; }
.modal h2 { margin: 0 0 16px; font-size: 20px; color: #333; }
.detail-meta { margin-bottom: 16px; }
.detail-meta p { margin: 4px 0; font-size: 14px; color: #666; }
.detail-req { background: #f9f9f9; border-radius: 6px; padding: 16px; margin-bottom: 20px; }
.detail-req h4 { margin: 0 0 8px; font-size: 14px; color: #333; }
.detail-req pre { margin: 0; font-size: 13px; color: #555; white-space: pre-wrap; font-family: inherit; }
.detail-submitted { background: #f6ffed; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
.detail-submitted h4 { margin: 0 0 8px; font-size: 14px; }
.detail-submitted p { margin: 4px 0; font-size: 13px; color: #666; }
.detail-actions { display: flex; gap: 8px; margin-top: 12px; }
.submit-section { background: #fffbe6; border-radius: 8px; padding: 16px; }
.submit-section h4 { margin: 0 0 12px; }
.submit-section input { margin-bottom: 12px; }
.btn-submit {
  padding: 10px 24px; background: #52c41a; color: #fff; border: none;
  border-radius: 6px; font-size: 14px; cursor: pointer; display: block;
}
.btn-submit:hover:not(:disabled) { background: #389e0d; }
.btn-submit:disabled { background: #b7eb8f; cursor: not-allowed; }
.grade-box { margin-top: 16px; padding-top: 16px; border-top: 1px solid #d9f0c5; }
.grade-box h4 { margin: 0 0 8px; }
.score { font-size: 22px; font-weight: 700; color: #1890ff; margin: 0; }
.comment {
  font-size: 14px; color: #666; font-style: italic; margin: 4px 0;
  cursor: pointer; transition: color 0.15s;
}
.comment:hover { color: #1890ff; }
.comment.truncated { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%; }
.comment:not(.truncated) { white-space: pre-wrap; word-break: break-word; }
.aigc { font-size: 12px; color: #999; margin: 4px 0 0; }
.annotated-section { margin-top: 16px; padding-top: 16px; border-top: 1px solid #d9f0c5; }
.annotated-section h4 { margin: 0 0 8px; font-size: 14px; }
.annotated-img {
  max-width: 100%; max-height: 200px; border-radius: 6px;
  border: 1px solid #e8e8e8; cursor: pointer; display: block; transition: opacity 0.2s;
}
.annotated-img:hover { opacity: 0.85; }

/* 提交记录 */
.submission-card {
  background: #fff; border-radius: 8px; padding: 16px 20px; display: flex;
  justify-content: space-between; align-items: flex-start;
  box-shadow: 0 1px 6px rgba(0,0,0,0.06); margin-bottom: 10px;
}
.sub-main h4 { margin: 0; font-size: 15px; color: #333; }
.sub-main p { margin: 4px 0; font-size: 13px; color: #888; }
.sub-actions { display: flex; gap: 8px; margin-top: 10px; }
.sub-grade { text-align: right; white-space: nowrap; min-width: 100px; }
.sub-grade .score { font-size: 18px; color: #1890ff; font-weight: 700; }
.sub-grade .pending { color: #fa8c16; font-size: 13px; }
.sub-grade .comment { font-size: 13px; color: #666; cursor: pointer; transition: color 0.15s; max-width: 720px; text-align: left; }
.sub-grade .comment:hover { color: #1890ff; }
.sub-grade .comment.truncated { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sub-grade .comment:not(.truncated) { white-space: pre-wrap; word-break: break-word; }
.sub-grade .aigc { font-size: 12px; color: #999; }

/* 个人资料 */
.profile-card {
  background: #fff; border-radius: 8px; padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06); max-width: 480px;
}
.profile-card h3 { margin: 0 0 20px; font-size: 18px; color: #333; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; color: #555; }
.form-group input {
  width: 100%; padding: 10px 12px; border: 1px solid #ddd;
  border-radius: 4px; font-size: 14px; box-sizing: border-box;
}
.form-group input:focus { outline: none; border-color: #52c41a; }
.form-group input:disabled { background: #f5f5f5; color: #999; }
.profile-card button {
  padding: 10px 24px; background: #52c41a; color: #fff;
  border: none; border-radius: 6px; font-size: 14px; cursor: pointer;
}
.profile-card button:hover:not(:disabled) { background: #389e0d; }
.profile-card button:disabled { background: #b7eb8f; cursor: not-allowed; }

/* 通用 */
.btn-sm { padding: 6px 12px; background: #4a90d9; color: #fff; border: none; border-radius: 4px; font-size: 12px; cursor: pointer; }
.btn-sm:hover { background: #357abd; }
.btn-outline { background: #fff; color: #4a90d9; border: 1px solid #4a90d9; }
.btn-outline:hover { background: #f0f7ff; }
.error { color: #f5222d; font-size: 14px; }
.success { color: #52c41a; font-size: 14px; margin-top: 12px; }
.empty { text-align: center; color: #999; padding: 48px 0; font-size: 15px; }
/* 成绩统计 */
.score-stats-card {
  background: #fff; border-radius: 10px; padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06); margin-bottom: 24px;
}
.avg-score-block {
  display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px;
}
.avg-label { font-size: 15px; color: #666; }
.avg-value { font-size: 36px; font-weight: 700; color: #1890ff; }
.avg-sub { font-size: 13px; color: #aaa; }
.line-chart-wrap { overflow-x: auto; }
.line-chart-svg { width: 100%; min-width: 400px; height: auto; }
.single-score { text-align: center; padding: 16px 0; }
.single-val { font-size: 28px; font-weight: 700; color: #1890ff; display: block; }
.single-name { font-size: 13px; color: #999; }

.tab-content { min-height: 300px; }
</style>
