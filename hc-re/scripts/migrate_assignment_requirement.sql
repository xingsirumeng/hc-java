-- 为 assignment 表添加 requirement 列
ALTER TABLE assignment ADD COLUMN IF NOT EXISTS requirement TEXT;
