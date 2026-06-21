package com.moodtune.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moodtune.entity.Song;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SongMapper extends BaseMapper<Song> {

    @Select("SELECT * FROM songs WHERE liked = true")
    List<Song> findByLikedTrue();

    @Select("SELECT * FROM songs WHERE genre = #{genre}")
    List<Song> findByGenre(String genre);

    @Select("SELECT * FROM songs WHERE title LIKE CONCAT('%', #{keyword}, '%') OR artist LIKE CONCAT('%', #{keyword}, '%')")
    List<Song> findByTitleContainingOrArtistContaining(String keyword);
}
