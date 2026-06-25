-- ============================================
-- 学生端功能迁移脚本
-- ============================================

-- 1. 为 assignment 表添加作业要求字段
ALTER TABLE assignment ADD COLUMN IF NOT EXISTS requirement TEXT;

-- 说明：
-- 学生密码的 BCrypt 升级由应用层在登录时自动完成（与教师登录逻辑一致），
-- 因此不需要批量 hash 已有密码。首次登录后密码即变为 BCrypt 格式。
