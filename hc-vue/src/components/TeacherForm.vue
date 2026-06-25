<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { addTeacher, updateTeacher } from '@/api/teacher'
import type { Teacher } from '@/types/teacher'

const props = defineProps<{
  editingTeacher: Teacher | null
}>()

const emit = defineEmits<{
  added: []
  updated: []
}>()

const form = reactive<Teacher>({
  teacherId: undefined,
  name: '',
  password: '',
})

const loading = ref(false)
const message = ref('')
const isEditing = ref(false)

// 监听父组件传入的 editingTeacher，填充表单
watch(() => props.editingTeacher, (teacher) => {
  if (teacher) {
    form.teacherId = teacher.teacherId
    form.name = teacher.name
    form.password = teacher.password
    isEditing.value = true
  } else {
    resetForm()
    isEditing.value = false
  }
})

function resetForm() {
  form.teacherId = undefined
  form.name = ''
  form.password = ''
}

function cancelEdit() {
  resetForm()
  isEditing.value = false
  emit('updated')
}

async function handleSubmit() {
  if (!form.teacherId || !form.name || !form.password) {
    message.value = '请填写所有字段'
    return
  }

  loading.value = true
  message.value = ''

  try {
    if (isEditing.value) {
      await updateTeacher(form.teacherId!, form)
      message.value = '修改成功！'
      emit('updated')
    } else {
      await addTeacher(form)
      message.value = '添加成功！'
      emit('added')
    }
    resetForm()
    isEditing.value = false
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    const action = isEditing.value ? '修改' : '添加'
    message.value = action + '失败: ' + detail
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="teacher-form">
    <h2>{{ isEditing ? '编辑教师' : '添加教师' }}</h2>

    <form @submit.prevent="handleSubmit">
      <div class="form-group">
        <label for="teacherId">教师ID（如 T001）</label>
        <input id="teacherId" v-model="form.teacherId" type="text" placeholder="请输入教师ID（如 T001）" :disabled="isEditing" />
      </div>

      <div class="form-group">
        <label for="name">姓名</label>
        <input id="name" v-model="form.name" type="text" placeholder="请输入教师姓名" />
      </div>

      <div class="form-group">
        <label for="password">密码</label>
        <input id="password" v-model="form.password" type="password" placeholder="请输入密码" />
      </div>

      <button type="submit" :disabled="loading">
        {{ loading ? '提交中...' : (isEditing ? '保存' : '添加') }}
      </button>

      <button v-if="isEditing" type="button" class="btn-cancel" @click="cancelEdit">取消</button>
    </form>

    <p v-if="message" class="msg" :class="{ error: message.includes('失败') }">
      {{ message }}
    </p>
  </div>
</template>

<style scoped>
.teacher-form {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.teacher-form h2 {
  margin: 0 0 20px;
  font-size: 18px;
  color: #333;
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
  transition: border-color 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: #4a90d9;
}

.form-group input:disabled {
  background: #f5f5f5;
  color: #999;
}

button {
  width: 100%;
  padding: 10px;
  background: #4a90d9;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s;
}

button:hover:not(:disabled) {
  background: #357abd;
}

button:disabled {
  background: #a0c4e8;
  cursor: not-allowed;
}

.btn-cancel {
  margin-top: 8px;
  background: #999;
}

.btn-cancel:hover:not(:disabled) {
  background: #777;
}

.msg {
  margin: 12px 0 0;
  font-size: 14px;
  color: #52c41a;
}

.msg.error {
  color: #f5222d;
}
</style>
