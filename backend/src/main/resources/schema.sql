-- 歌曲表
CREATE TABLE IF NOT EXISTS songs (
    id BIGSERIAL PRIMARY KEY,                                    -- 歌曲ID，主键，自增
    title VARCHAR(255) NOT NULL,                                 -- 歌曲标题
    artist VARCHAR(255) NOT NULL,                                -- 艺术家/歌手名称
    genre VARCHAR(100),                                          -- 音乐风格/流派（如：流行、摇滚、民谣等）
    file_url VARCHAR(500),                                       -- 歌曲文件URL（MinIO存储地址）
    liked BOOLEAN NOT NULL DEFAULT FALSE,                        -- 是否喜欢该歌曲（收藏标记）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP      -- 创建时间
);

-- 歌曲分组表（歌单/播放列表）
CREATE TABLE IF NOT EXISTS song_groups (
    id BIGSERIAL PRIMARY KEY,                                    -- 分组ID，主键，自增
    name VARCHAR(255) NOT NULL,                                  -- 分组名称（如：我喜欢的音乐、摇滚精选等）
    source VARCHAR(50) NOT NULL,                                 -- 来源类型（imported=从平台导入，manual=手动创建）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP      -- 创建时间
);

-- 歌曲-分组关联表（多对多关系）
CREATE TABLE IF NOT EXISTS song_group_relations (
    id BIGSERIAL PRIMARY KEY,                                    -- 关联ID，主键，自增
    song_id BIGINT NOT NULL,                                     -- 歌曲ID，外键关联songs表
    group_id BIGINT NOT NULL,                                    -- 分组ID，外键关联song_groups表
    UNIQUE(song_id, group_id)                                    -- 唯一约束：同一首歌在同一分组中只能出现一次
);

-- 导入记录表（从音乐平台导入歌单的历史记录）
CREATE TABLE IF NOT EXISTS song_imports (
    id BIGSERIAL PRIMARY KEY,                                    -- 导入记录ID，主键，自增
    source_link VARCHAR(500) NOT NULL,                           -- 源链接（音乐平台的歌单URL）
    platform VARCHAR(50) NOT NULL,                               -- 音乐平台名称（如：qq_music、netease、spotify等）
    status VARCHAR(20) NOT NULL,                                 -- 导入状态（pending=待处理，success=成功，failed=失败）
    imported_count INTEGER DEFAULT 0,                            -- 成功导入的歌曲数量
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP      -- 创建时间
);

-- 聊天会话表（与AI音乐助手的对话会话）
CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGSERIAL PRIMARY KEY,                                    -- 会话ID，主键，自增
    title VARCHAR(255) NOT NULL,                                 -- 会话标题（如：今天的心情、周末听歌等）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,     -- 创建时间
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP      -- 最后更新时间（用于排序显示最近会话）
);

-- 聊天消息表（会话中的每条消息记录）
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,                                    -- 消息ID，主键，自增
    session_id BIGINT NOT NULL,                                  -- 所属会话ID，外键关联chat_sessions表
    sender_type VARCHAR(20) NOT NULL,                            -- 发送者类型（user=用户，ai=AI助手）
    text_content TEXT,                                           -- 消息文本内容
    song_id BIGINT,                                              -- 推荐的歌曲ID（可选，仅AI消息中包含推荐时有值）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP      -- 消息创建时间
);

-- 索引（用于加速查询性能）
CREATE INDEX IF NOT EXISTS idx_songs_liked ON songs(liked);                                       -- 加速查询喜欢的歌曲
CREATE INDEX IF NOT EXISTS idx_songs_genre ON songs(genre);                                       -- 加速按音乐风格查询
CREATE INDEX IF NOT EXISTS idx_song_group_relations_song_id ON song_group_relations(song_id);    -- 加速查询歌曲所属的分组
CREATE INDEX IF NOT EXISTS idx_song_group_relations_group_id ON song_group_relations(group_id);  -- 加速查询分组中的歌曲列表
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages(session_id);            -- 加速查询会话的消息历史
CREATE INDEX IF NOT EXISTS idx_chat_sessions_updated_at ON chat_sessions(updated_at DESC);       -- 加速按最近更新时间排序会话列表
