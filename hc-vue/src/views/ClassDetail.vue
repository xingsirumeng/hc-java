<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAssignmentsByCourse } from '@/api/teacher'
import StudentList from '@/components/StudentList.vue'
import AssignmentList from '@/components/AssignmentList.vue'
import AssignmentForm from '@/components/AssignmentForm.vue'
import type { Assignment } from '@/types/teacher'

const route = useRoute()
const router = useRouter()
const courseId = route.params.courseId as string

const showForm = ref(false)
const editingAssignment = ref<Assignment | null>(null)
const assignments = ref<Assignment[]>([])

async function loadAssignments() {
  try {
    assignments.value = await getAssignmentsByCourse(courseId)
  } catch { /* ignore */ }
}

function onEdit(assignment: Assignment) {
  editingAssignment.value = assignment
  showForm.value = true
}

function onAssignmentCreated() {
  showForm.value = false
  editingAssignment.value = null
  loadAssignments()
}

function onCloseForm() {
  showForm.value = false
  editingAssignment.value = null
}

onMounted(loadAssignments)
</script>

<template>
  <div class="class-detail">
    <header class="header">
      <button class="btn-back" @click="router.push('/')">← 返回</button>
      <h1>{{ courseId }}</h1>
      <button class="btn-add" @click="showForm = true">+ 布置作业</button>
    </header>

    <div class="columns">
      <StudentList :course-id="courseId" />
      <AssignmentList ref="assignmentRef" :course-id="courseId" @edit="onEdit" />
    </div>

    <AssignmentForm
      v-if="showForm"
      :course-id="courseId"
      :edit-assignment="editingAssignment"
      @created="onAssignmentCreated"
      @close="onCloseForm"
    />
  </div>
</template>

<style scoped>
.class-detail {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 16px;
}

.header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.header h1 {
  margin: 0;
  font-size: 22px;
  color: #333;
  flex: 1;
}

.btn-back {
  padding: 6px 14px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-back:hover {
  background: #e8e8e8;
}

.btn-add {
  padding: 8px 20px;
  background: #4a90d9;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.btn-add:hover {
  background: #357abd;
}

.columns {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 24px;
  align-items: start;
}

@media (max-width: 768px) {
  .columns {
    grid-template-columns: 1fr;
  }
}
</style>
