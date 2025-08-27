package com.maihehe.blogcore._02_controller;

import com.maihehe.blogcore._03_service.INoteService;
import com.maihehe.blogcore._06_DTO.normalDTO.TreeNodeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/note/")
public class NoteController {
    @Autowired private INoteService noteService;


    // 根据 notePath，得到对应的 note 的 viewCount
    @GetMapping("viewCount")
    public ResponseEntity<Integer> getViewCount(@RequestParam("currentPath") String currentPath) {
        log.info("=============尝试获取 viewCount");
        try {
            Integer viewCount = noteService.loadViewCount(currentPath);
            log.info("=============viewCount 获取成功");
            return ResponseEntity.ok(viewCount);
        } catch (Exception e) {
            log.error("viewCount 获取失败：{}", e.getMessage());
            return ResponseEntity.status(500).body(0);
        }
    }
}