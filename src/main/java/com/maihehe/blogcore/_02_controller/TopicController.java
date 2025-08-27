package com.maihehe.blogcore._02_controller;

import com.maihehe.blogcore._03_service.ITopicService;
import com.maihehe.blogcore._06_DTO.normalDTO.TopicCategoryDTO;
import com.maihehe.blogcore._06_DTO.request.TopicCategoryMarkRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/")
public class TopicController {
    @Autowired private ITopicService topicService;

    @PostMapping("admin/topic/markCategory")
    public ResponseEntity<String> markCategory(@RequestBody List<TopicCategoryMarkRequest> requests) {
        log.info("=============进行 markCategory，标记Topic");
        try{
            topicService.markCategory(requests);
            log.info("=============标记完成！");
            return ResponseEntity.ok("标记完成！");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("标记失败：" + e.getMessage());
        }
    }

    @GetMapping("topic/topicCategories")
    public ResponseEntity<List<TopicCategoryDTO>> getTopicCategories() {
        log.info("=============尝试读取 topicCategories");
        try{
            List<TopicCategoryDTO> topicCategories = topicService.loadTopicCategory();
            log.info("=============成功返回 categories");
            return ResponseEntity.ok(topicCategories);
        } catch (Exception e){
            // 失败时返回空的列表
            log.error("获取 TopicCategoryDTO 失败：" + e.getMessage());
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

}
