<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAllAssignments, getAllStudents, uploadFile, submitAssignment } from '@/api/teacher'
import type { Assignment, Student } from '@/types/teacher'

const assignments = ref<Assignment[]>([])
const students = ref<Student[]>([])

const form = ref({
  studentId: '',
  assignmentName: '',
})
const selectedFile = ref<File | null>(null)
const loading = ref(false)
const message = ref('')
const isError = ref(false)

async function loadData() {
  try {
    const [a, s] = await Promise.all([getAllAssignments(), getAllStudents()])
    assignments.value = a
    students.value = s
  } catch (e: any) {
    showMsg('加载数据失败: ' + (e?.message || '未知错误'), true)
  }
}

function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files?.length) {
    selectedFile.value = input.files[0] as unknown as File
  }
}

function showMsg(msg: string, error = false) {
  message.value = msg
  isError.value = error
}

async function handleSubmit() {
  if (!form.value.studentId || !form.value.assignmentName || !selectedFile.value) {
    showMsg('请填写完整信息并选择文件', true)
    return
  }

  loading.value = true
  message.value = ''

  try {
    // 1. 上传文件
    showMsg('正在上传文件...')
    const uploadResult = await uploadFile(selectedFile.value)

    // 2. 提交作业
    showMsg('正在提交...')
    await submitAssignment({
      assignmentName: form.value.assignmentName,
      studentId: form.value.studentId,
      filepath: uploadResult.filepath,
      originalFilename: uploadResult.originalFilename,
    })

    showMsg('✅ 作业提交成功！')
    selectedFile.value = null
    // 清空文件选择
    const input = document.getElementById('fileInput') as HTMLInputElement
    if (input) input.value = ''
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    showMsg('提交失败: ' + detail, true)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="student-page">
    <div class="card">
      <h1>📝 学生作业提交（模拟端）</h1>
      <p class="subtitle">无需登录，模拟学生提交作业</p>

      <form @submit.prevent="handleSubmit">
        <!-- 选择学生 -->
        <div class="form-group">
          <label>选择学生</label>
          <select v-model="form.studentId">
            <option value="">-- 请选择 --</option>
            <option v-for="s in students" :key="s.studentId" :value="s.studentId">
              {{ s.studentId }} - {{ s.name }}
            </option>
          </select>
        </div>

        <!-- 选择作业 -->
        <div class="form-group">
          <label>选择作业</label>
          <select v-model="form.assignmentName">
            <option value="">-- 请选择 --</option>
            <option v-for="a in assignments" :key="a.name" :value="a.name">
              {{ a.name }}（{{ a.courseId }}）
            </option>
          </select>
        </div>

        <!-- 上传文件 -->
        <div class="form-group">
          <label>提交文件</label>
          <input
            id="fileInput"
            type="file"
            @change="onFileChange"
          />
        </div>

        <button type="submit" :disabled="loading">
          {{ loading ? '提交中...' : '提交作业' }}
        </button>
      </form>

      <p v-if="message" class="msg" :class="{ error: isError }">
        {{ message }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.student-page {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: #f0f2f5;
  padding: 40px 16px;
}

.card {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.08);
  width: 100%;
  max-width: 500px;
}

h1 {
  margin: 0 0 4px;
  font-size: 20px;
  color: #333;
}

.subtitle {
  margin: 0 0 28px;
  color: #999;
  font-size: 14px;
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

.form-group select,
.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
  background: #fff;
}

.form-group select:focus,
.form-group input:focus {
  outline: none;
  border-color: #4a90d9;
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

.msg {
  margin: 16px 0 0;
  font-size: 14px;
  color: #52c41a;
  text-align: center;
}

.msg.error {
  color: #f5222d;
}
</style>
