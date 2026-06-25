-- 为 submission 表添加 annotated_filepath 列
ALTER TABLE submission ADD COLUMN IF NOT EXISTS annotated_filepath VARCHAR(1024) DEFAULT NULL;
