import axios from 'axios'
import type { Teacher, Result, LoginData, StudentLoginData, Student, Assignment, Submission, StudentAssignmentDTO, Course } from '@/types/teacher'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// ========== 请求拦截器：自动携带 Token ==========
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ========== 响应拦截器：401/403 时清除 Token 并根据角色跳转 ==========
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      const role = localStorage.getItem('userRole')
      // 只清除 token，保留 userRole 用于正确跳转
      localStorage.removeItem('token')
      localStorage.removeItem('teacherName')
      localStorage.removeItem('studentName')
      localStorage.removeItem('studentId')

      const loginPath = role === 'student' ? '/login/student' : '/login'
      if (window.location.pathname !== loginPath) {
        window.location.href = loginPath
      }
    }
    if (status === 403) {
      // 403 不跳转，只清除 token（角色不匹配，让页面自行处理）
      localStorage.removeItem('token')
      localStorage.removeItem('teacherName')
      localStorage.removeItem('studentName')
      localStorage.removeItem('studentId')
    }
    return Promise.reject(error)
  },
)

// ================================================================
// 登录
// ================================================================

/** 教师登录 */
export async function login(teacherId: string, password: string): Promise<LoginData> {
  const res = await api.post<Result<LoginData>>('/login', { teacherId, password })
  if (res.data.code !== 1 || !res.data.data) {
    throw new Error(res.data.msg || '登录失败')
  }
  return res.data.data
}

/** 学生登录 */
export async function studentLogin(studentId: string, password: string): Promise<StudentLoginData> {
  const res = await api.post<Result<StudentLoginData>>('/login/student', { studentId, password })
  if (res.data.code !== 1 || !res.data.data) {
    throw new Error(res.data.msg || '登录失败')
  }
  return res.data.data
}

// ================================================================
// 教师 CRUD
// ================================================================

export async function getAllTeachers(): Promise<Teacher[]> {
  const res = await api.get<Result<Teacher[]>>('/teacher')
  return res.data.data
}

export async function addTeacher(teacher: Teacher): Promise<string> {
  const res = await api.post<Result<string>>('/teacher/add', teacher)
  return res.data.data
}

export async function getTeacherById(teacherId: string): Promise<Teacher> {
  const res = await api.get<Result<Teacher>>(`/teacher/${teacherId}`)
  return res.data.data
}

export async function updateTeacher(teacherId: string, teacher: Teacher): Promise<string> {
  const res = await api.put<Result<string>>(`/teacher/${teacherId}`, teacher)
  return res.data.data
}

export async function deleteTeacher(teacherId: string): Promise<string> {
  const res = await api.delete<Result<string>>(`/teacher/${teacherId}`)
  return res.data.data
}

/** 获取当前教师个人资料 */
export async function getTeacherProfile(): Promise<{ teacherId: string; name: string }> {
  const res = await api.get<Result<{ teacherId: string; name: string }>>('/teacher/profile')
  return res.data.data
}

/** 更新当前教师个人资料 */
export async function updateTeacherProfile(data: { name?: string; password?: string }): Promise<string> {
  const res = await api.put<Result<string>>('/teacher/profile', data)
  return res.data.data
}

// ================================================================
// 课程（教师视角即"班级"）
// ================================================================

export async function getMyCourses(): Promise<Course[]> {
  const res = await api.get<Result<Course[]>>('/courses/my')
  return res.data.data
}


// ================================================================
// 学生（教师端查询）
// ================================================================

export async function getStudentsByCourse(courseId: string): Promise<Student[]> {
  const res = await api.get<Result<Student[]>>(`/student/by-course/${encodeURIComponent(courseId)}`)
  return res.data.data
}

// ================================================================
// 作业
// ================================================================

export async function getAssignmentsByCourse(courseId: string): Promise<Assignment[]> {
  const res = await api.get<Result<Assignment[]>>('/assignment', { params: { courseId } })
  return res.data.data
}

export async function createAssignment(data: Partial<Assignment>): Promise<string> {
  const res = await api.post<Result<string>>('/assignment', data)
  return res.data.data
}

export async function updateAssignment(assignmentName: string, data: Partial<Assignment>): Promise<string> {
  const res = await api.put<Result<string>>(`/assignment/${encodeURIComponent(assignmentName)}`, data)
  return res.data.data
}

export async function deleteAssignment(assignmentName: string): Promise<string> {
  const res = await api.delete<Result<string>>(`/assignment/${encodeURIComponent(assignmentName)}`)
  return res.data.data
}

export async function getSubmissions(assignmentName: string): Promise<Submission[]> {
  const res = await api.get<Result<Submission[]>>(`/assignment/${assignmentName}/submissions`)
  return res.data.data
}

// ================================================================
// 批改
// ================================================================

export async function gradeSubmission(
  assignmentName: string,
  studentId: string,
  score: number | null,
  comment: string | null,
  annotatedFilepath?: string | null,
  gradeAttachments?: string | null,
): Promise<string> {
  const res = await api.put<Result<string>>('/submission/grade', {
    assignmentName,
    studentId,
    score,
    comment,
    annotatedFilepath: annotatedFilepath || null,
    gradeAttachments: gradeAttachments || null,
  })
  return res.data.data
}

/** 打回作业 */
export async function returnSubmission(
  assignmentName: string,
  studentId: string,
  reason?: string,
): Promise<string> {
  const res = await api.put<Result<string>>('/submission/return', {
    assignmentName,
    studentId,
    reason: reason || null,
  })
  return res.data.data
}

/** 批量批改作业 */
export async function batchGradeSubmissions(
  grades: Array<{
    assignmentName: string
    studentId: string
    score: number | null
    comment: string | null
    annotatedFilepath?: string | null
    gradeAttachments?: string | null
  }>,
): Promise<string> {
  const res = await api.put<Result<string>>('/submission/batch-grade', { grades })
  return res.data.data
}

/** 批量删除作业 */
export async function batchDeleteAssignments(names: string[]): Promise<string> {
  const res = await api.delete<Result<string>>('/assignment/batch', { data: { names } })
  return res.data.data
}

// ================================================================
// 延期管理
// ================================================================

/** 获取某作业的所有延期记录 */
export async function getExtensions(assignmentName: string): Promise<import('@/types/teacher').DeadlineExtension[]> {
  const res = await api.get<Result<import('@/types/teacher').DeadlineExtension[]>>(
    `/assignment/${encodeURIComponent(assignmentName)}/extensions`,
  )
  return res.data.data
}

/** 为单个学生延长提交时间 */
export async function extendDeadline(
  assignmentName: string,
  studentId: string,
  extendedEndTime: string,
  reason?: string,
): Promise<string> {
  const res = await api.post<Result<string>>(
    `/assignment/${encodeURIComponent(assignmentName)}/extend`,
    { studentId, extendedEndTime, reason: reason || null },
  )
  return res.data.data
}

/** 批量为学生延长提交时间 */
export async function batchExtendDeadline(
  assignmentName: string,
  extensions: Array<{ studentId: string; extendedEndTime: string; reason?: string }>,
): Promise<string> {
  const res = await api.post<Result<string>>(
    `/assignment/${encodeURIComponent(assignmentName)}/extend/batch`,
    { extensions },
  )
  return res.data.data
}

/** 撤销学生的延期 */
export async function revokeExtension(assignmentName: string, studentId: string): Promise<string> {
  const res = await api.delete<Result<string>>(
    `/assignment/${encodeURIComponent(assignmentName)}/extend/${encodeURIComponent(studentId)}`,
  )
  return res.data.data
}

// ================================================================
// 相似度查询
// ================================================================

/** 查询作业相似度结果 */
export async function getSimilarities(
  assignmentName: string,
  studentId?: string,
): Promise<import('@/types/teacher').AssignmentSimilarity[]> {
  const res = await api.get<Result<import('@/types/teacher').AssignmentSimilarity[]>>(
    `/assignment/${encodeURIComponent(assignmentName)}/similarity`,
    { params: studentId ? { studentId } : {} },
  )
  return res.data.data
}

/** 触发相似度计算（异步），返回 { totalPairs, message } */
export async function computeSimilarities(assignmentName: string): Promise<{ totalPairs: number; message: string }> {
  const res = await api.post<Result<{ totalPairs: number; message: string }>>(
    `/assignment/${encodeURIComponent(assignmentName)}/similarity/compute`,
  )
  return res.data.data
}

// ================================================================
// 学生端（需要学生 JWT）
// ================================================================

/** 获取学生个人资料 */
export async function getStudentProfile(): Promise<{ studentId: string; name: string }> {
  const res = await api.get<Result<{ studentId: string; name: string }>>('/student/profile')
  return res.data.data
}

/** 更新学生个人资料 */
export async function updateStudentProfile(data: { name?: string; password?: string }): Promise<string> {
  const res = await api.put<Result<string>>('/student/profile', data)
  return res.data.data
}

/** 获取学生选修的课程 */
export async function getStudentCourses(): Promise<Course[]> {
  const res = await api.get<Result<Course[]>>('/student/courses')
  return res.data.data
}

/** 获取学生的作业列表（含提交状态） */
export async function getStudentAssignments(): Promise<StudentAssignmentDTO[]> {
  const res = await api.get<Result<StudentAssignmentDTO[]>>('/student/assignments')
  return res.data.data
}

/** 获取单个作业详情（含提交状态） */
export async function getStudentAssignmentDetail(assignmentName: string): Promise<StudentAssignmentDTO> {
  const res = await api.get<Result<StudentAssignmentDTO>>(`/student/assignments/${encodeURIComponent(assignmentName)}`)
  return res.data.data
}

/** 提交作业 */
export async function submitStudentAssignment(
  assignmentName: string,
  filepath: string,
  originalFilename: string,
): Promise<string> {
  const res = await api.post<Result<string>>(`/student/assignments/${encodeURIComponent(assignmentName)}/submit`, {
    filepath,
    originalFilename,
  })
  return res.data.data
}

/** 获取学生的所有提交记录 */
export async function getStudentSubmissions(): Promise<Submission[]> {
  const res = await api.get<Result<Submission[]>>('/student/submissions')
  return res.data.data
}

/** 获取学生某个作业的提交详情 */
export async function getStudentSubmissionDetail(assignmentName: string): Promise<Submission> {
  const res = await api.get<Result<Submission>>(`/student/submissions/${encodeURIComponent(assignmentName)}`)
  return res.data.data
}

/** 获取学生已出分的作业 */
export async function getStudentGrades(): Promise<Submission[]> {
  const res = await api.get<Result<Submission[]>>('/student/grades')
  return res.data.data
}

// ================================================================
// AI 评阅
// ================================================================

export async function aiGrade(filepath: string, requirement: string): Promise<{
  score: number
  commentary: string
  extracted_text?: string
}> {
  const res = await api.post<Result<{
    score: number
    commentary: string
    extracted_text?: string
  }>>('/ai/grade', { filepath, requirement })
  if (res.data.code !== 1 || !res.data.data) {
    throw new Error(res.data.msg || 'AI 评阅失败')
  }
  return res.data.data
}

export async function aiAigcDetect(
  filepath: string,
  assignmentName: string,
  studentId: string,
): Promise<{
  aigc_score: number
  suggestion: string
  label: string
}> {
  const res = await api.post<Result<{
    aigc_score: number
    suggestion: string
    label: string
  }>>('/ai/aigc-detect-file', { filepath, assignmentName, studentId })
  if (res.data.code !== 1 || !res.data.data) {
    throw new Error(res.data.msg || 'AIGC 检测失败')
  }
  return res.data.data
}

// ================================================================
// 文件
// ================================================================

/** 获取文件 blob 和 Content-Type，供组件自行渲染 */
export async function fetchFileBlob(filepath: string): Promise<{ blob: Blob; contentType: string }> {
  const res = await api.get('/file/preview', {
    params: { filepath },
    responseType: 'blob',
  })
  return {
    blob: res.data,
    contentType: res.headers['content-type'] || 'application/octet-stream',
  }
}

export async function downloadFile(filepath: string, filename: string): Promise<void> {
  const res = await api.get('/file/download', {
    params: { filepath },
    responseType: 'blob',
  })
  const url = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

export async function previewFile(filepath: string): Promise<void> {
  const res = await api.get('/file/preview', {
    params: { filepath },
    responseType: 'blob',
  })
  const url = URL.createObjectURL(res.data)
  window.open(url, '_blank')
}

export async function uploadFile(file: File): Promise<{ filepath: string; originalFilename: string }> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await api.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data
}

// ================================================================
// 模拟学生端（无需登录，测试用）
// ================================================================

export async function getAllAssignments(): Promise<Assignment[]> {
  const res = await api.get<Result<Assignment[]>>('/student-mock/assignments')
  return res.data.data
}

export async function getAllStudents(): Promise<Student[]> {
  const res = await api.get<Result<Student[]>>('/student-mock/students')
  return res.data.data
}

export async function submitAssignment(data: {
  assignmentName: string
  studentId: string
  filepath: string
  originalFilename: string
}): Promise<string> {
  const res = await api.post<Result<string>>('/student-mock/submit', data)
  return res.data.data
}
