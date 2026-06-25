<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStudentsByCourse } from '@/api/teacher'
import type { Student } from '@/types/teacher'

const props = defineProps<{ courseId: string }>()

const students = ref<Student[]>([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    students.value = await getStudentsByCourse(props.courseId)
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    error.value = '加载失败: ' + detail
  } finally {
    loading.value = false
  }
}

defineExpose({ refresh: load })
onMounted(load)
</script>

<template>
  <div class="student-list">
    <h3>课程学生</h3>

    <p v-if="loading" class="status">加载中...</p>
    <p v-else-if="error" class="status error">{{ error }}</p>
    <p v-else-if="!students.length" class="status">暂无学生</p>

    <table v-else>
      <thead>
        <tr>
          <th>学号</th>
          <th>姓名</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="s in students" :key="s.studentId">
          <td>{{ s.studentId }}</td>
          <td>{{ s.name }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.student-list {
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

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}

th {
  background: #fafafa;
  color: #555;
}
</style>
