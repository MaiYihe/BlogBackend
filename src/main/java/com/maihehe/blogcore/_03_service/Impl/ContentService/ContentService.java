package com.maihehe.blogcore._03_service.Impl.ContentService;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.maihehe.blogcore._04_mapper.NoteMapper;
import com.maihehe.blogcore._05_entity.Note;
import com.maihehe.blogcore._06_DTO.normalDTO.NoteCachedDTO;
import com.maihehe.blogcore._05_entity.Topic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ContentService {
    @Autowired private SqlUpdater sqlUpdater;
    @Autowired private CacheReader cacheReader;
    @Autowired private PathReader pathReader;
    @Autowired private NoteMapper noteMapper;

    @Transactional
    @Caching(evict = {
            // detailedContent 的 key 是 #p0（参数动态），这里直接清整个缓存分区更省事
            @CacheEvict(cacheNames = "detailedContent", allEntries = true, beforeInvocation = true),
            // treeNodeAZ 只清理固定 key 'treeNodes'
            @CacheEvict(cacheNames = "treeNodeAZ", key = "'treeNodes'", beforeInvocation = true)
    })
    public void scanData() {
        /** 第一步，扫描本地文件路径与数据库路径，对数据库进行更新 **/
        log.info("====对 Topic 进行操作");
        // 从本地文件里面，读取出 currentTopics（Topic 实体类列表）
        List<Topic> currentTopics = pathReader.readTopics();
        log.info("当前 topic 列表：{}", currentTopics);
        // 从数据库或中读取出 topicCachePaths
        List<String> topicCachePaths = cacheReader.readTopicCache();
        log.info("当前 cachedTopic 列表{}", topicCachePaths);
        // 比较本地与数据库(缓存)，更新数据库
        sqlUpdater.topicTable(topicCachePaths, currentTopics);

        log.info("====对 Notes 进行操作");
        List<Note> currentNotes = pathReader.readNotesInTopics();

        // 数据库/缓存：一次性读取所有 Note 的缓存记录
        List<NoteCachedDTO> notePathCacheList = cacheReader.readNoteCache();
        List<String> allNotePathsInTable = notePathCacheList.stream()
                .map(NoteCachedDTO::getCurrentPath)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 记录一些统计日志，便于排查
        log.info("本地扫描到 Note 数量：{}", currentNotes.size());
        log.info("数据库缓存中的 Note 数量：{}", allNotePathsInTable.size());

        //  交给 SQL 更新器做增删（参数1=数据库已有的 currentPath 列表；参数2=本地扫描的 Note 列表）
        sqlUpdater.noteTable(allNotePathsInTable, currentNotes);

    }


    @Transactional
    @Cacheable(cacheNames="detailedContent", key="#p0")
    public String loadDetailedContent(String notePath) throws Exception {
        Path filePath = Paths.get(notePath);
        log.info("尝试加载文章详细内容，路径: {}", filePath);
        // 访问的不是文件，抛出异常
        if (Files.isDirectory(filePath)) {
            throw new IllegalArgumentException("路径是一个文件夹，不是文件：" + filePath);
        }


        Note note = noteMapper.selectOne(
                new QueryWrapper<Note>().eq("current_path", notePath));

        boolean isAdmin = isCurrentUserAdmin();
        if (!isAdmin && Boolean.FALSE.equals(note.getVisible())) {
            // 对游客/非管理员隐藏：返回 401
            throw new AuthenticationCredentialsNotFoundException("需要管理员权限");
        }

        // 访问增加计数、方法2：原子化操作更新数据表，线程安全
        noteMapper.update(
                null,
                new LambdaUpdateWrapper<Note>()
                        .eq(Note::getCurrentPath, notePath)
                        .setSql("view_count = view_count + 1")
        );
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        // 你的 JWT 里放的是 "authorities": ["ROLE_ADMIN", ...]
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}