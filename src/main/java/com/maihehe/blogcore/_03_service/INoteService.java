package com.maihehe.blogcore._03_service;

import com.maihehe.blogcore._05_entity.Note;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maihehe.blogcore._06_DTO.normalDTO.TreeNodeDTO;

import java.util.List;

/**
 * <p>
 * 笔记内容表 服务类
 * </p>
 *
 * @author maihehe
 * @since 2025-05-21
 */
public interface INoteService extends IService<Note> {


    List<Note> loadNotes();

    Integer loadViewCount(String currentPath) throws Exception;

}
