package com.maihehe.blogcore._03_service.Impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maihehe.blogcore._03_service.INoteService;
import com.maihehe.blogcore._03_service.ITopicService;
import com.maihehe.blogcore._03_service.ITreeNodeService;
import com.maihehe.blogcore._04_mapper.NoteMapper;
import com.maihehe.blogcore._04_mapper.TopicMapper;
import com.maihehe.blogcore._05_entity.Note;
import com.maihehe.blogcore._05_entity.Topic;
import com.maihehe.blogcore._06_DTO.normalDTO.TreeNodeDTO;
import com.maihehe.blogcore._06_DTO.request.TreeNodeVisibilityUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TreeNodeServiceImpl implements ITreeNodeService {
    @Autowired private INoteService noteService;
    @Autowired private ITopicService topicService;
    @Autowired private NoteMapper noteMapper;
    @Autowired private TopicMapper topicMapper;

    // 任务1
    @Override
    @Cacheable(cacheNames = "treeNodeAZ", key = "'treeNodes'")
    public List<TreeNodeDTO> loadTreeByAZ() {
        List<Topic> topics = topicService.loadTopicByAZ();
        TopicTreeBuilder topicTreeBuilder = new TopicTreeBuilder();
        List<Note> notes = noteService.loadNotes();
        return topicTreeBuilder.buildTreeByAZ(topics, notes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVisibility(List<TreeNodeVisibilityUpdateRequest> items) {
        if (items == null || items.isEmpty()) return;

        for (var req : items) {
            boolean vis = Boolean.TRUE.equals(req.getVisible());
            String path = req.getPath();

            switch (req.getNodeType()) {
                case TOPIC -> {
                    int rows = topicMapper.update(
                            null,
                            Wrappers.<Topic>lambdaUpdate()
                                    .eq(Topic::getPath, path)
                                    .set(Topic::getVisible, vis)
                    );
                    if (rows == 0) {
                        throw new IllegalStateException("Topic 不存在，path=" + path);
                    }
                }
                case NOTE -> {
                    int rows = noteMapper.update(
                            null,
                            Wrappers.<Note>lambdaUpdate()
                                    .eq(Note::getCurrentPath, path)
                                    .set(Note::getVisible, vis)
                    );
                    if (rows == 0) {
                        throw new IllegalStateException("Note 不存在，path=" + path);
                    }
                }
                default -> throw new IllegalArgumentException("不支持的节点类型: " + req.getNodeType());
            }
        }
    }

    // 内部类
    private static class TopicTreeBuilder {
        // 任务1：根据 TopicAZ 进行排序
        /**
         * 把 Topic 转化为 DTO
         */
        private TreeNodeDTO convertTopicToTreeNodeDTO(Topic topic) {
            TreeNodeDTO treeNodeDTO = new TreeNodeDTO();
            treeNodeDTO.setId(topic.getId());
            treeNodeDTO.setCategory(topic.getCategory());
            treeNodeDTO.setFolder(true);
            treeNodeDTO.setCurrentPath(topic.getPath());
            treeNodeDTO.setName(topic.getName());
            treeNodeDTO.setVisible(topic.getVisible());
            treeNodeDTO.setChildren(null);
            return treeNodeDTO;
        }

        /**
         * 把 Note 转化为 DTO
         */
        private TreeNodeDTO convertNoteToTreeNodeDTO(Note note) {
            TreeNodeDTO treeNodeDTO = new TreeNodeDTO();
            treeNodeDTO.setId(note.getId());
            treeNodeDTO.setFolder(note.getIsFolder());
            treeNodeDTO.setParentPath(note.getParentPath()); // 设置 ParentPath
            treeNodeDTO.setCurrentPath(note.getCurrentPath());
            String name = note.getName();
            if (name != null && name.endsWith(".md")) {
                name = name.substring(0, name.length() - 3); // 去掉最后 3 个字符
            }
            treeNodeDTO.setName(name);
            treeNodeDTO.setViewCount(note.getViewCount());
            treeNodeDTO.setVisible(note.getVisible());
            treeNodeDTO.setChildren(null);
            return treeNodeDTO;
        }

        /**
         * 创建 currentPath -> DTO 的映射表
         */
        private Map<String, TreeNodeDTO> mapPathAndDTO(List<TreeNodeDTO> treeNodes) {
            Map<String, TreeNodeDTO> dtoMap = new HashMap<>();
            for (TreeNodeDTO treeNode : treeNodes) {
                dtoMap.put(treeNode.getCurrentPath(), treeNode);
            }
            return dtoMap;
        }

        /**
         * 构建 Topic(AZ排序) 根节点的树结构
         */
        public List<TreeNodeDTO> buildTreeByAZ(List<Topic> topics, List<Note> notes) {
            // 将 topics 转化为 TreeNodeDTO
            List<TreeNodeDTO> treeNodes = new ArrayList<>();
            for (Topic topic : topics) {
                TreeNodeDTO nodeFromTopic = convertTopicToTreeNodeDTO(topic);
                treeNodes.add(nodeFromTopic);
            }
            // 将 notes 转化为 TreeNodeDTO
            for (Note note : notes) {
                TreeNodeDTO nodeFromNote = convertNoteToTreeNodeDTO(note);
                treeNodes.add(nodeFromNote);
            }

            // 所有节点映射
            Map<String, TreeNodeDTO> pathToDtoMap = mapPathAndDTO(treeNodes);

            // 建立父子关系
            List<TreeNodeDTO> rootList = new ArrayList<>();
            for (TreeNodeDTO treeNode : treeNodes) {
                String parentPath = treeNode.getParentPath();
                TreeNodeDTO parentNode = pathToDtoMap.get(parentPath);
                TreeNodeDTO currentNode = pathToDtoMap.get(treeNode.getCurrentPath());
                // 如果父节点找不到，则挂到根节点
                if (parentNode == null) {
                    // 没有父节点，是根节点
                    rootList.add(currentNode);
                } else {
                    // 是子节点，添加到父节点的 children 中
                    safeAddChild(parentNode, currentNode);
                }
            }
            return rootList;
        }

        private static void safeAddChild(TreeNodeDTO parent, TreeNodeDTO child){
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(child);
        }
    }
}
