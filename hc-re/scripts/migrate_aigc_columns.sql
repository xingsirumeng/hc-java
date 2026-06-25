-- 为已有 submission 表添加 AIGC 检测结果列
ALTER TABLE submission ADD COLUMN IF NOT EXISTS aigc_score INT DEFAULT NULL;
