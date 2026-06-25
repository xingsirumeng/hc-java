<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import StudentManage from '@/components/admin/StudentManage.vue'
import TeacherManage from '@/components/admin/TeacherManage.vue'
import CourseManage from '@/components/admin/CourseManage.vue'
import AssignmentManage from '@/components/admin/AssignmentManage.vue'
import SubmissionView from '@/components/admin/SubmissionView.vue'
import EnrollmentManage from '@/components/admin/EnrollmentManage.vue'

const router = useRouter()

const tabs = [
  { key: 'students', label: '学生管理' },
  { key: 'teachers', label: '教师管理' },
  { key: 'courses', label: '课程管理' },
  { key: 'assignments', label: '作业管理' },
  { key: 'submissions', label: '提交记录' },
  { key: 'enrollments', label: '选课管理' },
]

const activeTab = ref('students')

function goHome() {
  router.push('/')
}
</script>

<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <h2 class="sidebar-title">管理后台</h2>
      <nav class="sidebar-nav">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="['nav-item', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </nav>
      <div class="sidebar-footer">
        <button class="nav-item home-btn" @click="goHome">← 返回首页</button>
      </div>
    </aside>
    <main class="main-content">
      <StudentManage v-if="activeTab === 'students'" />
      <TeacherManage v-if="activeTab === 'teachers'" />
      <CourseManage v-if="activeTab === 'courses'" />
      <AssignmentManage v-if="activeTab === 'assignments'" />
      <SubmissionView v-if="activeTab === 'submissions'" />
      <EnrollmentManage v-if="activeTab === 'enrollments'" />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 200px;
  background: #001529;
  color: #fff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-title {
  padding: 20px 16px 12px;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-nav {
  flex: 1;
  padding: 8px 0;
}

.nav-item {
  display: block;
  width: 100%;
  padding: 12px 24px;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
}

.nav-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.nav-item.active {
  color: #fff;
  background: #1890ff;
}

.sidebar-footer {
  padding: 12px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.home-btn {
  font-size: 13px;
}

.main-content {
  flex: 1;
  padding: 24px;
  background: #f0f2f5;
  overflow-y: auto;
}
</style>
