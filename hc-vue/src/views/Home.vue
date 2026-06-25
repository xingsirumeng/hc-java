<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyCourses, getTeacherProfile, updateTeacherProfile } from '@/api/teacher'
import type { Course } from '@/types/teacher'

const router = useRouter()
const courses = ref<Course[]>([])
const loading = ref(false)
const error = ref('')
const teacherName = ref(localStorage.getItem('teacherName') || '')

// 个人资料
const showProfile = ref(false)
const profileTeacherId = ref('')
const profileForm = reactive({ name: '', password: '', confirmPassword: '' })
const profileSaving = ref(false)
const profileMsg = ref('')
const profileError = ref(false)

async function loadCourses() {
  loading.value = true
  error.value = ''
  try {
    courses.value = await getMyCourses()
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    error.value = '加载失败: ' + detail
  } finally {
    loading.value = false
  }
}

async function openProfile() {
  try {
    const profile = await getTeacherProfile()
    teacherName.value = profile.name
    profileTeacherId.value = profile.teacherId
    profileForm.name = profile.name
    profileForm.password = ''
    profileForm.confirmPassword = ''
    profileMsg.value = ''
    showProfile.value = true
  } catch { /* ignore */ }
}

async function handleSaveProfile() {
  profileMsg.value = ''
  profileError.value = false

  if (profileForm.password && profileForm.password !== profileForm.confirmPassword) {
    profileMsg.value = '两次输入的密码不一致'
    profileError.value = true
    return
  }

  profileSaving.value = true
  try {
    const data: { name?: string; password?: string } = {}
    if (profileForm.name && profileForm.name !== teacherName.value) {
      data.name = profileForm.name
    }
    if (profileForm.password) {
      data.password = profileForm.password
    }
    if (!data.name && !data.password) {
      profileMsg.value = '没有需要修改的内容'
      profileError.value = true
      return
    }

    await updateTeacherProfile(data)
    if (data.name) {
      teacherName.value = data.name
      localStorage.setItem('teacherName', data.name)
    }
    profileForm.password = ''
    profileForm.confirmPassword = ''
    profileMsg.value = '✅ 资料更新成功'
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    profileMsg.value = '保存失败: ' + detail
    profileError.value = true
  } finally {
    profileSaving.value = false
  }
}

function goToCourse(c: Course) {
  router.push(`/course/${encodeURIComponent(c.courseId)}`)
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userRole')
  localStorage.removeItem('teacherName')
  router.push('/login')
}

onMounted(loadCourses)

</script>

<template>
  <div class="home">
    <header class="header">
      <h1>教师工作台</h1>
      <div class="header-right">
        <span class="teacher-name" @click="openProfile" title="点击查看个人资料">{{ teacherName }}</span>
        <button class="btn-logout" @click="logout">退出</button>
      </div>
    </header>

    <h2 class="section-title">我的课程</h2>

    <p v-if="loading" class="status">加载中...</p>
    <p v-else-if="error" class="status error">{{ error }}</p>
    <p v-else-if="!courses.length" class="status">暂无课程数据</p>

    <div v-else class="class-grid">
      <div
        v-for="c in courses"
        :key="c.courseId"
        class="class-card"
        @click="goToCourse(c)"
      >
        <h3>{{ c.courseName }}</h3>
        <p>{{ c.courseId }} · {{ c.credit }} 学分</p>
      </div>
    </div>

    <!-- 个人资料弹窗 -->
    <div v-if="showProfile" class="modal-overlay" @click.self="showProfile = false">
      <div class="modal">
        <h2>个人资料</h2>
        <div class="form-group">
          <label>教师 ID</label>
          <input :value="profileTeacherId" type="text" disabled />
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
        <button class="btn-cancel" @click="showProfile = false">取消</button>
        <p v-if="profileMsg" :class="profileError ? 'error-msg' : 'success-msg'">{{ profileMsg }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 16px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.header h1 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #888;
  font-size: 14px;
}

.teacher-name {
  cursor: pointer;
  color: #4a90d9;
}

.teacher-name:hover {
  text-decoration: underline;
}

.btn-logout {
  padding: 6px 16px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #666;
}

.btn-logout:hover {
  background: #e8e8e8;
}

.section-title {
  margin: 0 0 20px;
  font-size: 18px;
  color: #333;
}

.status {
  font-size: 14px;
  color: #999;
}

.status.error {
  color: #f5222d;
}

.class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.class-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}

.class-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.class-card h3 {
  margin: 0 0 8px;
  color: #333;
  font-size: 16px;
}

.class-card p {
  margin: 0;
  color: #888;
  font-size: 13px;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 80px;
  z-index: 100;
}

.modal {
  background: #fff;
  border-radius: 8px;
  padding: 32px;
  width: 440px;
  max-width: 90vw;
}

.modal h2 {
  margin: 0 0 20px;
  font-size: 18px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #555;
}

.form-group input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #4a90d9;
}

.form-group input:disabled {
  background: #f5f5f5;
  color: #999;
}

.modal button {
  width: 100%;
  padding: 10px;
  background: #4a90d9;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 15px;
  cursor: pointer;
}

.modal button:disabled {
  background: #a0c4e8;
  cursor: not-allowed;
}

.btn-cancel {
  margin-top: 8px;
  background: #999 !important;
}

.btn-cancel:hover {
  background: #777 !important;
}

.error-msg {
  margin: 12px 0 0;
  color: #f5222d;
  font-size: 14px;
}

.success-msg {
  margin: 12px 0 0;
  color: #52c41a;
  font-size: 14px;
}
</style>
