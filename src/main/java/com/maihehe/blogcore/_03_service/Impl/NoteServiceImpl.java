package com.maihehe.blogcore._03_service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maihehe.blogcore._03_service.ITopicService;
import com.maihehe.blogcore._05_entity.Note;
import com.maihehe.blogcore._04_mapper.NoteMapper;
import com.maihehe.blogcore._03_service.INoteService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maihehe.blogcore._05_entity.Topic;
import com.maihehe.blogcore._06_DTO.normalDTO.TreeNodeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * <p>
 * 笔记内容表 服务实现类
 * </p>
 *
 * @author maihehe
 * @since 2025-05-21
 */

@Slf4j
@Service
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements INoteService {
    @Autowired ITopicService topicService;
    @Autowired NoteMapper noteMapper;



    @Override
    public List<Note> loadNotes() {
        // 查询表中所有 Notes
        return this.list();
    }

    @Override
    public Integer loadViewCount(String currentPath) throws IllegalArgumentException {
        Note note = noteMapper.selectOne(
                new QueryWrapper<Note>().eq("current_path", currentPath)
        );
        if (note == null) {
            throw new IllegalArgumentException("未找到 currentPath 为 " + currentPath + " 的记录");
        }
        return note.getViewCount();
    }

}
