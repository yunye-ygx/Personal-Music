package com.moodtune.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moodtune.entity.ChatMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("SELECT * FROM chat_messages WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    @Delete("DELETE FROM chat_messages WHERE session_id = #{sessionId}")
    void deleteBySessionId(Long sessionId);
}
