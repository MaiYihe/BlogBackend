package com.maihehe.blogcore._03_service.Impl.ContentService;

import com.maihehe.blogcore._03_service.ITopicService;
import com.maihehe.blogcore._04_mapper.NoteMapper;
import com.maihehe.blogcore._05_entity.Topic;
import com.maihehe.blogcore._06_DTO.normalDTO.NoteCachedDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CacheReader {

    @Autowired
    private ITopicService topicService;
    @Autowired
    private NoteMapper noteMapper;


    List<String> readTopicCache() {
        List<String> fallbackData = topicService.loadTopics()
                .stream()
                .map(Topic::getPath)
                .collect(Collectors.toList());

        return fallbackData;
    }

    List<NoteCachedDTO> readNoteCache() {
        List<NoteCachedDTO> noteDTOS = noteMapper.selectNotesWithPaths(); //包含 topicPath、currentPath 两个主要信息
        return noteDTOS != null ? noteDTOS : new ArrayList<>();
    }

}

