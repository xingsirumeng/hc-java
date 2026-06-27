<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { createAssignment, updateAssignment } from '@/api/teacher'
import { useToast } from '@/composables/useToast'
import type { Assignment } from '@/types/teacher'

const { showSuccess, showError } = useToast()

const props = defineProps<{
  courseId: string
  editAssignment?: Assignment | null
}>()

const emit = defineEmits<{
  created: []
  close: []
}>()

const isEdit = !!props.editAssignment

const form = reactive({
  name: '',
  startTime: '',
  endTime: '',
  requirement: '',
})

const loading = ref(false)

function toDatetimeLocal(iso: string): string {
  const d = new Date(iso)
  const offset = d.getTimezoneOffset()
  const local = new Date(d.getTime() - offset * 60000)
  return local.toISOString().slice(0, 16)
}

onMounted(() => {
  if (props.editAssignment) {
    form.name = props.editAssignment.name
    form.startTime = toDatetimeLocal(props.editAssignment.startTime as any)
    form.endTime = toDatetimeLocal(props.editAssignment.endTime as any)
    form.requirement = props.editAssignment.requirement || ''
  }
})

async function handleSubmit() {
  if (!form.name || !form.startTime || !form.endTime) {
    showError('请填写所有字段')
    return
  }

  loading.value = true

  try {
    const data = {
      name: form.name,
      startTime: new Date(form.startTime).toISOString(),
      endTime: new Date(form.endTime).toISOString(),
      courseId: props.courseId,
      requirement: form.requirement || null,
    }
    if (isEdit) {
      await updateAssignment(props.editAssignment!.name, data)
      showSuccess('作业修改成功')
    } else {
      await createAssignment(data)
      showSuccess('作业布置成功')
    }
    emit('created')
  } catch (e: any) {
    const detail = e?.response?.data?.msg || e?.message || '未知错误'
    showError((isEdit ? '修改失败: ' : '布置失败: ') + detail)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="modal">
      <h2>{{ isEdit ? '修改作业' : '布置作业' }}</h2>

      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label>作业名称</label>
          <input v-model="form.name" type="text" placeholder="如：DS Homework 3" :disabled="isEdit" />
        </div>

        <div class="form-group">
          <label>作业要求</label>
          <textarea v-model="form.requirement" rows="3" placeholder="输入作业要求（可选）"></textarea>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>开始时间</label>
            <input v-model="form.startTime" type="datetime-local" />
          </div>
          <div class="form-group">
            <label>截止时间</label>
            <input v-model="form.endTime" type="datetime-local" />
          </div>
        </div>

        <button type="submit" :disabled="loading">
          {{ loading ? '提交中...' : (isEdit ? '保存修改' : '确认布置') }}
        </button>
        <button type="button" class="btn-cancel" @click="emit('close')">取消</button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  background: #fff;
  border-radius: 8px;
  padding: 32px;
  width: 440px;
  max-width: 90vw;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

h2 {
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

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group textarea {
  resize: vertical;
  font-family: inherit;
}

.form-row {
  display: flex;
  gap: 12px;
}

.form-row .form-group {
  flex: 1;
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
}

button:disabled {
  background: #a0c4e8;
  cursor: not-allowed;
}

.btn-cancel {
  margin-top: 8px;
  background: #999;
}

.error-msg {
  margin: 12px 0 0;
  color: #f5222d;
  font-size: 14px;
}
</style>
