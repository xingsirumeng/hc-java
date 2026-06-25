import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import StudentLogin from '@/views/StudentLogin.vue'
import Home from '@/views/Home.vue'
import ClassDetail from '@/views/ClassDetail.vue'
import StudentDashboard from '@/views/StudentDashboard.vue'
import StudentView from '@/views/StudentView.vue'
import AdminDashboard from '@/views/AdminDashboard.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 教师
    {
      path: '/login',
      name: 'Login',
      component: Login,
    },
    {
      path: '/',
      name: 'Home',
      component: Home,
      meta: { requiresAuth: true },
    },
    {
      path: '/course/:courseId',
      name: 'ClassDetail',
      component: ClassDetail,
      meta: { requiresAuth: true },
    },

    // 学生
    {
      path: '/login/student',
      name: 'StudentLogin',
      component: StudentLogin,
    },
    {
      path: '/student',
      name: 'StudentDashboard',
      component: StudentDashboard,
      meta: { requiresStudentAuth: true },
    },

    // 模拟学生端（无需登录，测试用）
    {
      path: '/student-mock',
      name: 'StudentMock',
      component: StudentView,
    },

    // 管理员后台（无需登录，可直接访问）
    {
      path: '/admin',
      name: 'AdminDashboard',
      component: AdminDashboard,
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('userRole')

  // 教师页面
  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }
  if (to.meta.requiresAuth && token && role === 'student') {
    // 学生 token 无法访问教师页面
    next('/student')
    return
  }

  // 学生页面
  if (to.meta.requiresStudentAuth && !token) {
    next('/login/student')
    return
  }
  if (to.meta.requiresStudentAuth && token && role === 'teacher') {
    // 教师 token 无法访问学生页面
    next('/')
    return
  }

  next()
})

export default router
