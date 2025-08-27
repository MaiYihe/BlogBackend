package com.maihehe.blogcore._03_service.Impl.ContentService;

import com.maihehe.blogcore._04_mapper.NoteMapper;
import com.maihehe.blogcore._04_mapper.TopicMapper;
import com.maihehe.blogcore._05_entity.Note;
import com.maihehe.blogcore._05_entity.Topic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SqlUpdater {
    @Autowired private TopicMapper topicMapper;
    @Autowired private NoteMapper noteMapper;

    class CompareResult {
        Set<String> before;
        Set<String> current;
        Set<String> added;
        Set<String> removed;
        //constructor
        CompareResult(Set<String> before, Set<String> current) {
            this.before = before;
            this.current = current;

            this.added = new HashSet<>(current);
            this.added.removeAll(before);
            this.removed = new HashSet<>(before);
            this.removed.removeAll(current);
        }
    }

    @Transactional
    void topicTable(List<String> topicCacheList, List<Topic> currentTopics) {
        Set<String> topicCacheSet = new HashSet<>(topicCacheList);
        Map<String,Topic> pathToTopic = currentTopics.stream()
                .collect(Collectors.toMap(Topic::getPath, topic -> topic));
        Set<String> currentSet = pathToTopic.keySet();

        CompareResult result = new CompareResult(topicCacheSet, currentSet);
        Set<String> added = result.added;
        Set<String> removed = result.removed;

        // 执行新增操作
        if (!added.isEmpty()) {
            List<Topic> topicToInsert = added.stream()
                    .map(pathToTopic::get)  // ← 对每个路径字符串，查 Map 得到对应的 Note
                    .toList();
            log.info("准备在数据库新增 " + topicToInsert.size() + " 个 Topic");
            topicMapper.batchInsert(topicToInsert);
        }
        // 执行删除操作

        if (!removed.isEmpty()) {
            List<Topic> topicToDelete = removed.stream()
                    .map(p -> { Topic t = new Topic(); t.setPath(p); return t; })
                    .toList();
            log.info("准备在数据库删除 " + topicToDelete.size() + " 个 Topic");
            topicMapper.deleteByPaths(topicToDelete);
        }
    }

    // currentNotes 意思是，当前某个 Topic 下的 Notes
    @Transactional
    void noteTable(List<String> allNotePathsInTable, List<Note> currentNotes) {
        Set<String> noteCacheSet = new HashSet<>(allNotePathsInTable);
        // 本地目录下的 path - note 哈希表
        Map<String, Note> pathToNote = currentNotes.stream()
                .collect(Collectors.toMap(Note::getCurrentPath, note -> note));
        Set<String> currentSet = pathToNote.keySet();

        CompareResult result = new CompareResult(noteCacheSet, currentSet);
        Set<String> added = result.added;
        Set<String> removed = result.removed;

        // 执行新增操作
        if (!added.isEmpty()) {
            List<Note> notesToInsert = added.stream()
                    .map(pathToNote::get)  // ← 对每个路径字符串，查 Map 得到对应的 Note
                    .collect(Collectors.toList());
            log.info("准备新增 " + notesToInsert.size() + " 个 Note");
            noteMapper.batchInsert(notesToInsert);  // 示例：你的数据库插入逻辑
        }

        // 执行删除操作
        if (!removed.isEmpty()) {
            List<Note> notesToDelete = removed.stream()
                    .map(p -> { Note n = new Note(); n.setCurrentPath(p); return n; }) // ★ 关键：别用 pathToNote::get
                    .toList();

            log.info("准备删除 {} 个 Note", notesToDelete.size());

            final int BATCH = 800;
            for (int i = 0; i < notesToDelete.size(); i += BATCH) {
                List<Note> slice = notesToDelete.subList(i, Math.min(i + BATCH, notesToDelete.size()));
                int affected = noteMapper.deleteByPaths(slice);
                log.debug("本批删除 {} 条，受影响 {}", slice.size(), affected);
            }
        }
    }
}

