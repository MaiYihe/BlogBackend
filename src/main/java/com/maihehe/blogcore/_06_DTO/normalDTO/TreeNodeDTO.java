package com.maihehe.blogcore._06_DTO.normalDTO;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// 这是一个自引用类
// 后端只需要返回一个递归控制的子树，如何将内容渲染为导航栏，就是前端应该考虑的问题
@Data
public class TreeNodeDTO {
    private String id = null;
    // 判断是否是文件夹
    private boolean folder = true;
    private String category = null; // 只有 topic 节点才能为他赋值
    private String parentPath = null; // 用于遍历找到 children
    private String currentPath = null;
    private String name;
    private Integer viewCount = 0;
    private Boolean visible = true;
    private List<TreeNodeDTO> children = new ArrayList<>(); // 用于 JSON 输出
}