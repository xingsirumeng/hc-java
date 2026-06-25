<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { studentLogin } from '@/api/teacher'

const router = useRouter()

const form = reactive({
  studentId: '',
  password: '',
})

const loading = ref(false)
const errorMsg = ref('')

// 进入登录页时清除所有旧会话，防止过期 token 干扰
onMounted(() => {
  localStorage.removeItem('token')
  localStorage.removeItem('userRole')
  localStorage.removeItem('teacherName')
  localStorage.removeItem('studentName')
  localStorage.removeItem('studentId')
})

async function handleLogin() {
  if (!form.studentId || !form.password) {
    errorMsg.value = '请输入学号和密码'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    const data = await studentLogin(form.studentId, form.password)
    localStorage.setItem('token', data.token)
    localStorage.setItem('userRole', 'student')
    localStorage.setItem('studentName', data.student.name)
    localStorage.setItem('studentId', data.student.studentId)
    router.push('/student')
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    errorMsg.value = '登录失败: ' + detail
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1>🎓 学生端</h1>
      <h2>登录</h2>

      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="studentId">学号</label>
          <input
            id="studentId"
            v-model="form.studentId"
            type="text"
            placeholder="请输入学号（如 202201010001）"
            autocomplete="username"
          />
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </div>

        <button type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

      <div class="teacher-link">
        <router-link to="/login">👨‍🏫 教师端登录</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}

.login-card {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

.login-card h1 {
  text-align: center;
  color: #333;
  font-size: 22px;
  margin: 0 0 8px;
}

.login-card h2 {
  text-align: center;
  color: #666;
  font-size: 16px;
  margin: 0 0 28px;
  font-weight: 400;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #555;
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: #52c41a;
}

button {
  width: 100%;
  padding: 12px;
  background: #52c41a;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.2s;
}

button:hover:not(:disabled) {
  background: #389e0d;
}

button:disabled {
  background: #b7eb8f;
  cursor: not-allowed;
}

.error-msg {
  margin: 16px 0 0;
  font-size: 14px;
  color: #f5222d;
  text-align: center;
}

.teacher-link {
  margin-top: 24px;
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.teacher-link a {
  color: #4a90d9;
  font-size: 14px;
  text-decoration: none;
}

.teacher-link a:hover {
  text-decoration: underline;
}
</style>
