import axios from 'axios'
import type { Student, Teacher, Course, Assignment, Submission, Result } from '@/types/teacher'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// ================================================================
// 统计
// ================================================================

export interface AdminStats {
  studentCount: number
  teacherCount: number
  courseCount: number
  assignmentCount: number
  submissionCount: number
  enrollmentCount: number
}

export async function getAdminStats(): Promise<AdminStats> {
  const res = await api.get<Result<AdminStats>>('/admin/stats')
  return res.data.data
}

// ================================================================
// 学生 CRUD
// ================================================================

export async function getStudents(): Promise<Student[]> {
  const res = await api.get<Result<Student[]>>('/admin/students')
  return res.data.data
}

export async function getStudent(studentId: string): Promise<Student> {
  const res = await api.get<Result<Student>>(`/admin/students/${encodeURIComponent(studentId)}`)
  return res.data.data
}

export async function addStudent(data: Student): Promise<string> {
  const res = await api.post<Result<string>>('/admin/students', data)
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

export async function updateStudent(studentId: string, data: Partial<Student>): Promise<string> {
  const res = await api.put<Result<string>>(`/admin/students/${encodeURIComponent(studentId)}`, data)
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

export async function deleteStudent(studentId: string): Promise<string> {
  const res = await api.delete<Result<string>>(`/admin/students/${encodeURIComponent(studentId)}`)
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

// ================================================================
// 教师 CRUD
// ================================================================

export async function getTeachers(): Promise<Teacher[]> {
  const res = await api.get<Result<Teacher[]>>('/admin/teachers')
  return res.data.data
}

export async function getTeacher(teacherId: string): Promise<Teacher> {
  const res = await api.get<Result<Teacher>>(`/admin/teachers/${encodeURIComponent(teacherId)}`)
  return res.data.data
}

export async function addTeacher(data: Teacher): Promise<string> {
  const res = await api.post<Result<string>>('/admin/teachers', data)
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

export async function updateTeacher(teacherId: string, data: Partial<Teacher>): Promise<string> {
  const res = await api.put<Result<string>>(`/admin/teachers/${encodeURIComponent(teacherId)}`, data)
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

export async function deleteTeacher(teacherId: string): Promise<string> {
  const res = await api.delete<Result<string>>(`/admin/teachers/${encodeURIComponent(teacherId)}`)
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

// ================================================================
// 课程 CRUD
// ================================================================

export async function getCourses(): Promise<Course[]> {
  const res = await api.get<Result<Course[]>>('/admin/courses')
  return res.data.data
}

export async function getCourse(courseId: string): Promise<Course> {
  const res = await api.get<Result<Course>>(`/admin/courses/${encodeURIComponent(courseId)}`)
  return res.data.data
}

export async function addCourse(data: Course): Promise<string> {
  const res = await api.post<Result<string>>('/admin/courses', data)
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

export async function updateCourse(courseId: string, data: Partial<Course>): Promise<string> {
  const res = await api.put<Result<string>>(`/admin/courses/${encodeURIComponent(courseId)}`, data)
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

export async function deleteCourse(courseId: string): Promise<string> {
  const res = await api.delete<Result<string>>(`/admin/courses/${encodeURIComponent(courseId)}`)
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

// ================================================================
// 作业 CRUD
// ================================================================

export async function getAssignments(courseId?: string): Promise<Assignment[]> {
  const res = await api.get<Result<Assignment[]>>('/admin/assignments', {
    params: courseId ? { courseId } : {},
  })
  return res.data.data
}

export async function getAssignment(name: string): Promise<Assignment> {
  const res = await api.get<Result<Assignment>>(`/admin/assignments/${encodeURIComponent(name)}`)
  return res.data.data
}

export async function addAssignment(data: Partial<Assignment>): Promise<string> {
  const res = await api.post<Result<string>>('/admin/assignments', data)
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

export async function updateAssignment(name: string, data: Partial<Assignment>): Promise<string> {
  const res = await api.put<Result<string>>(`/admin/assignments/${encodeURIComponent(name)}`, data)
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

export async function deleteAssignment(name: string): Promise<string> {
  const res = await api.delete<Result<string>>(`/admin/assignments/${encodeURIComponent(name)}`)
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

// ================================================================
// 提交记录
// ================================================================

export async function getSubmissions(assignmentName?: string, studentId?: string): Promise<Submission[]> {
  const res = await api.get<Result<Submission[]>>('/admin/submissions', {
    params: { assignmentName, studentId },
  })
  return res.data.data
}

export async function deleteSubmission(assignmentName: string, studentId: string): Promise<string> {
  const res = await api.delete<Result<string>>('/admin/submissions', {
    data: { assignmentName, studentId },
  })
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

/** 批量删除提交记录 */
export async function batchDeleteSubmissions(
  submissions: Array<{ assignmentName: string; studentId: string }>,
): Promise<string> {
  const res = await api.delete<Result<string>>('/admin/submissions/batch', {
    data: { submissions },
  })
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

// ================================================================
// 选课管理 (Sc)
// ================================================================

export interface ScRecord {
  scId: number
  studentId: string
  courseId: string
}

export async function getEnrollments(courseId?: string, studentId?: string): Promise<ScRecord[]> {
  const res = await api.get<Result<ScRecord[]>>('/admin/enrollments', {
    params: { courseId, studentId },
  })
  return res.data.data
}

export async function addEnrollment(studentId: string, courseId: string): Promise<string> {
  const res = await api.post<Result<string>>('/admin/enrollments', { studentId, courseId })
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}

export async function deleteEnrollment(scId: number): Promise<string> {
  const res = await api.delete<Result<string>>(`/admin/enrollments/${scId}`)
  if (res.data.code !== 1) throw new Error(res.data.msg)
  return res.data.data
}
