-- ============================================
-- 迁移：教师批改标注文件支持
-- 为 submission 表添加 annotated_filepath 列
-- 执行方式：psql -U postgres -d hc -f scripts/001_add_annotated_filepath.sql
-- ============================================

-- 检查列是否已存在，不存在则添加
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'submission' AND column_name = 'annotated_filepath'
    ) THEN
        ALTER TABLE submission ADD COLUMN annotated_filepath VARCHAR(500);
        RAISE NOTICE '已添加 annotated_filepath 列';
    ELSE
        RAISE NOTICE 'annotated_filepath 列已存在，跳过';
    END IF;
END $$;
