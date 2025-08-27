package com.maihehe.blogcore._03_service;

import com.maihehe.blogcore._06_DTO.normalDTO.TreeNodeDTO;
import com.maihehe.blogcore._06_DTO.request.TopicCategoryMarkRequest;
import com.maihehe.blogcore._06_DTO.request.TreeNodeVisibilityUpdateRequest;

import java.util.List;

public interface ITreeNodeService {
    List<TreeNodeDTO> loadTreeByAZ();
    void updateVisibility(List<TreeNodeVisibilityUpdateRequest> items);
}
