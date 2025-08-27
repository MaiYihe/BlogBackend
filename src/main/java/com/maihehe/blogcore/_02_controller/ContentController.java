package com.maihehe.blogcore._02_controller;

import com.maihehe.blogcore._03_service.Impl.ContentService.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.nio.file.NoSuchFileException;

@Slf4j
@RestController
@RequestMapping("/api/")

@Tag(name = "ContentController 本地文档管理")
public class ContentController {
    @Autowired
    private ContentService contentService;

    @Operation(summary = "扫描本地文档",description = "作用是把本地的文档，(路径内容)扫描到数据库里面去")
    @PostMapping("/admin/content/scan")
    public ResponseEntity<String> scanToUpdate() {
        log.info("=============进行ContentScan，扫描data目录");
        try {
            contentService.scanData();
            log.info("=============ContentScan扫描结束！");
            return ResponseEntity.ok("内容扫描完成！");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("扫描失败：" + e.getMessage());
        }
    }

    @GetMapping("/content/noteDetailedContent")
    public ResponseEntity<String> getNoteDetailedContent(@RequestParam("notePath") String notePath) {
        log.info("=============尝试根据 notePath 加载文章详细的内容数据");
        try{
            String detailedContent = contentService.loadDetailedContent(notePath);
            log.info("=============读取文章详情成功");
            return ResponseEntity.ok(detailedContent);
        } catch (NoSuchFileException e) { // 文件不存在或被隐藏等统一当成不存在
            log.info("文章不存在或不可访问: {}", notePath);
            return ResponseEntity.status(404).body("Not Found");
        } catch (Exception e){
            log.info("=============读取文章详情失败");
            return ResponseEntity.status(500).body("获取文章详情失败：" + e.getMessage());
        }
    }
}
