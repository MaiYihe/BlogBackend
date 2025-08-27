package com.maihehe.blogcore._02_controller;

import com.maihehe.blogcore._03_service.INoteService;
import com.maihehe.blogcore._03_service.ITopicService;
import com.maihehe.blogcore._03_service.ITreeNodeService;
import com.maihehe.blogcore._06_DTO.normalDTO.TreeNodeDTO;
import com.maihehe.blogcore._06_DTO.request.TreeNodeVisibilityUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/")
public class TreeNodeController {
    @Autowired ITreeNodeService TreeNodeService;

    // 返回所有的 topicPath 兼 notePath，根据 Topic 的字母进行排序；默认是 ByTopicsAZ 获取到 noteTree 的
    @GetMapping("/treeNode/noteTree")
    public ResponseEntity<List<TreeNodeDTO>> noteTreeByTopicsAZ(){
        log.info("=============尝试获取 note 树(By AZ)");
        try{
            List<TreeNodeDTO> noteTreeNodeDTOSAZ = TreeNodeService.loadTreeByAZ();
            return ResponseEntity.ok(noteTreeNodeDTOSAZ);
        } catch (Exception e){
            log.error("note 树获取失败：{}", e.getMessage());
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    @PostMapping("/admin/treeNode/visibility")
    public ResponseEntity<String> updateVisibility(@RequestBody List<TreeNodeVisibilityUpdateRequest> requests){
        log.info("=============尝试更新 TreeNode 的可见性");
        try{
            TreeNodeService.updateVisibility(requests);
            return ResponseEntity.ok("TreeNode 可见性更新成功");
        } catch (Exception e){
            log.error("TreeNode 可见性更新失败：{}", e.getMessage());
            return ResponseEntity.status(500).body("可见性更新失败");
        }
    }
}
