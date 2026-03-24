package com.maihehe.blogcore._03_service.Impl.ContentService;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.maihehe.blogcore._04_mapper.NoteMapper;
import com.maihehe.blogcore._05_entity.Note;
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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;
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

        // 数据库：一次性读取所有 Note 的元信息（用于增量判断）
        List<Note> dbNotes = noteMapper.selectList(
                new QueryWrapper<Note>().select("current_path", "is_folder", "file_mtime", "file_size", "content_hash")
        );
        Map<String, Note> dbNoteByPath = dbNotes.stream()
                .filter(n -> n.getCurrentPath() != null)
                .collect(Collectors.toMap(Note::getCurrentPath, n -> n, (a, b) -> a));
        List<String> allNotePathsInTable = new ArrayList<>(dbNoteByPath.keySet());

        // 本地：构建 path -> Note
        Map<String, Note> localNoteByPath = currentNotes.stream()
                .filter(n -> n.getCurrentPath() != null)
                .collect(Collectors.toMap(Note::getCurrentPath, n -> n, (a, b) -> a));

        // 先为新增的 Note 预先计算 hash（只对文件）
        Set<String> addedPaths = new HashSet<>(localNoteByPath.keySet());
        addedPaths.removeAll(dbNoteByPath.keySet());
        for (String pathStr : addedPaths) {
            Note note = localNoteByPath.get(pathStr);
            if (note == null || Boolean.TRUE.equals(note.getIsFolder())) {
                continue;
            }
            String hash = md5HexSafe(Paths.get(pathStr));
            note.setContentHash(hash);
        }

        // 记录一些统计日志，便于排查
        log.info("本地扫描到 Note 数量：{}", currentNotes.size());
        log.info("数据库缓存中的 Note 数量：{}", allNotePathsInTable.size());

        //  交给 SQL 更新器做增删（参数1=数据库已有的 currentPath 列表；参数2=本地扫描的 Note 列表）
        sqlUpdater.noteTable(allNotePathsInTable, currentNotes);

        // 更新已有 Note 的元信息/内容 hash（路径相同的记录）
        int metaUpdated = 0;
        int contentUpdated = 0;
        for (Note local : currentNotes) {
            String path = local.getCurrentPath();
            if (path == null) continue;
            Note db = dbNoteByPath.get(path);
            if (db == null) continue; // 新增的记录在上面已插入，不在这轮 DB 快照里

            boolean metaChanged = !Objects.equals(local.getFileMtime(), db.getFileMtime())
                    || !Objects.equals(local.getFileSize(), db.getFileSize());
            boolean isFolder = Boolean.TRUE.equals(local.getIsFolder());
            boolean hashMissing = !isFolder && (db.getContentHash() == null || db.getContentHash().isBlank());

            String hash = null;
            boolean hashChanged = false;
            if (!isFolder && (metaChanged || hashMissing)) {
                hash = md5HexSafe(Paths.get(path));
                if (hash != null) {
                    hashChanged = !hash.equals(db.getContentHash());
                }
            }

            boolean needUpdate = metaChanged || hashMissing || hashChanged;
            if (needUpdate) {
                noteMapper.update(
                        null,
                        new LambdaUpdateWrapper<Note>()
                                .eq(Note::getCurrentPath, path)
                                .set(Note::getFileMtime, local.getFileMtime())
                                .set(Note::getFileSize, local.getFileSize())
                                .set(!isFolder && hash != null, Note::getContentHash, hash)
                );
                metaUpdated++;
                if (hashChanged) contentUpdated++;
            }
        }
        log.info("Note 元信息更新 {} 条，其中内容变更 {} 条", metaUpdated, contentUpdated);

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

    private String md5HexSafe(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            log.warn("计算 MD5 失败: {}", path, e);
            return null;
        }
    }
}
