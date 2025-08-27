package com.maihehe.blogcore._04_mapper;

import com.maihehe.blogcore._05_entity.Note;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maihehe.blogcore._06_DTO.normalDTO.NoteCachedDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 笔记内容表 Mapper 接口
 * </p>
 *
 * @author maihehe
 * @since 2025-05-21
 */
@Mapper
public interface NoteMapper extends BaseMapper<Note> {

    @Select("SELECT id AS id, topic_path AS topicPath, current_path AS currentPath FROM maihehe_note")
    List<NoteCachedDTO> selectNotesWithPaths();

    // 往数据表中插入 notes 记录
    void batchInsert(@Param("notes") List<Note> notes);

    // 根据路径删除数据表的内容
    int deleteByPaths(@Param("notes") List<Note> notes);

}
