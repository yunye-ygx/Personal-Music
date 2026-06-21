-- V2__add_mood_tags.sql
-- 添加情绪标签字段到songs表

ALTER TABLE songs ADD COLUMN IF NOT EXISTS mood_tags TEXT[];

-- 创建GIN索引支持数组查询
CREATE INDEX IF NOT EXISTS idx_songs_mood_tags ON songs USING GIN(mood_tags);

-- 为现有歌曲添加示例mood_tags（可选，用于测试）
UPDATE songs SET mood_tags = ARRAY['未分类'] WHERE mood_tags IS NULL;
