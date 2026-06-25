export interface Teacher {
  teacherId?: string
  name: string
  password: string
}

/** 后端统一响应格式 */
export interface Result<T> {
  code: number    // 1=成功, 0=失败
  msg: string
  data: T
}

// ========== 登录 ==========

/** 教师登录返回 */
export interface LoginData {
  token: string
  teacher: {
    teacherId: string
    name: string
  }
}

/** 学生登录返回 */
export interface StudentLoginData {
  token: string
  student: {
    studentId: string
    name: string
  }
}

// ========== 课程 ==========

export interface Course {
  courseId: string
  courseName: string
  teacherId: string
  credit: number
}

// ========== 学生 ==========

export interface Student {
  studentId: string
  name: string
  password: string
}

// ========== 作业 ==========

export interface Assignment {
  name: string
  startTime: string
  endTime: string
  courseId: string
  requirement?: string
}

/** 学生端作业视图（含提交状态） */
export interface StudentAssignmentDTO {
  assignmentName: string
  courseId: string
  courseName: string
  startTime: string
  endTime: string
  requirement: string | null
  submitted: boolean
  submissionTime: string | null
  filepath: string | null
  originalFilename: string | null
  score: number | null
  comment: string | null
  aigcScore: number | null
  annotatedFilepath: string | null
}

// ========== 提交记录 ==========

export interface Submission {
  assignmentName: string
  studentId: string
  submissionTime: string
  filepath: string
  originalFilename: string
  score: number | null
  comment: string | null
  aigcScore: number | null
  annotatedFilepath: string | null
}
