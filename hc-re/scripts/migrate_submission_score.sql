-- 为已有 submission 表添加 score 和 comment 列（适用于已建库的情况）
ALTER TABLE submission ADD COLUMN IF NOT EXISTS score INT DEFAULT NULL;
ALTER TABLE submission ADD COLUMN IF NOT EXISTS comment TEXT DEFAULT NULL;
