package com.maihehe.blogcore._03_service.Impl.ContentService;


import com.maihehe.blogcore._01_config.ContentScan.NoteScanRulesService;
import com.maihehe.blogcore._05_entity.Note;
import com.maihehe.blogcore._05_entity.Topic;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class PathReader {
    private static final Path DATA_DIR = Paths.get("data");
    private final List<Topic> topics;

    private final NoteScanRulesService skipService; // ← 构造器注入更好
    public PathReader(NoteScanRulesService skipService) {
        this.skipService = skipService;

        // 构造 topics
        List<Topic> topicList = new ArrayList<>();
        try (Stream<Path> s = Files.list(DATA_DIR)) {
            List<Path> topicDirs = s
                    .filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS))
                    .filter(p -> !skipService.shouldSkip(p))     // 顶层也按规则跳
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .toList();
            for (Path fullPath : topicDirs) {
                Topic topic = new Topic();
                topic.setPath(fullPath.toString());               // 或者相对路径：DATA_DIR.relativize(fullPath).toString()
                topic.setName(fullPath.getFileName().toString());
                topic.setCategory(null);
                topic.setVisible(true);
                topic.setUpdatedTime(LocalDateTime.now());
                topicList.add(topic);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("读取数据目录失败: " + DATA_DIR, e);
        }
        this.topics = topicList;
    }

    /**
     * 读取 Topics
     * @return
     */
    public List<Topic> readTopics() {
        return this.topics;
    }


    /**
     * 读取 Topics 下，所有 List<Note> 的拼接
     * @return
     */
    public List<Note> readNotesInTopics() {
        List<Path> topicPaths = topics.stream()
                .map(Topic::getPath)          // 从 Topic 取出 String 路径
                .map(Paths::get)              // String → Path
                .toList();                    // 收集为 List<Path>
        List<Note> allNotes = topicPaths.stream()
                .map(this::readNotesInTopic)   // Path -> List<Note>
                .flatMap(List::stream)         // List<Note> -> Note
                .toList();                     // 收集为 List<Note>

        return allNotes;
    }
    private List<Note> readNotesInTopic(Path topicPath) {
        List<Note> subNotes = readSubNotes(topicPath, topicPath);
        List<Note> resultNote = new ArrayList<>();
        resultNote.addAll(subNotes);
        return resultNote;
    }
    // 读取出当前 topic 目录以下的，所有的子目录 notes（不包含根目录）
    private List<Note> readSubNotes(Path currentPath, Path topicPath) {
        List<Note> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(currentPath)) {
            for (Path path : stream.toList()) {
                // 判断文件名是否满足要求
                if (skipService.shouldSkip(path)) continue;

                boolean isDirectory = Files.isDirectory(path);
                Note note = new Note();
                note.setTopicPath(topicPath.toString());
                note.setName(path.getFileName().toString());
                note.setViewCount(0);
                note.setCurrentPath(path.toString());
                note.setVisible(true);// 默认可见
                note.setIsFolder(isDirectory);

                // ❗ 这里用 path.getParent() 取当前文件/文件夹的父目录
                Path parent = path.getParent();
                note.setParentPath(parent != null ? parent.toString() : null);

                note.setUpdatedTime(LocalDateTime.now());
                result.add(note);
                if (isDirectory) {
                    result.addAll(readSubNotes(path, topicPath));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * 读取 Topics 下，所有 -1_figures 的文件的拼接
     * @return
     */
    public List<Path> readTopicFigures(){
        List<Path> topicPaths = topics.stream()
                .map(Topic::getPath)          // 从 Topic 取出 String 路径
                .map(Paths::get)              // String → Path
                .toList();                    // 收集为 List<Path>

        List<Path> allFigurePaths = topicPaths.stream()
                .map(topicPath -> topicPath.resolve("-1_figures"))        // 拼成子目录路径
                .filter(Files::isDirectory)                               // 只保留存在的目录
                .flatMap(figDir -> {
                    try (Stream<Path> s = Files.list(figDir)) {
                        return s.filter(Files::isRegularFile).toList().stream();  // 列出文件
                    } catch (IOException e) {
                        e.printStackTrace();
                        return Stream.empty();    // 出错就忽略这个目录
                    }
                })
                .toList();
        return allFigurePaths;
    }
}