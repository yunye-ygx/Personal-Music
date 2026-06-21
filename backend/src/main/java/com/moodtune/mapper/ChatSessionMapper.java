package com.moodtune.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moodtune.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    @Select("SELECT * FROM chat_sessions ORDER BY updated_at DESC")
    List<ChatSession> findAllByOrderByUpdatedAtDesc();
}
