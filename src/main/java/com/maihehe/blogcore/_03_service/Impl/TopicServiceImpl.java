package com.maihehe.blogcore._03_service.Impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maihehe.blogcore._05_entity.Topic;
import com.maihehe.blogcore._04_mapper.TopicMapper;
import com.maihehe.blogcore._03_service.ITopicService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maihehe.blogcore._06_DTO.normalDTO.TopicCategoryDTO;
import com.maihehe.blogcore._06_DTO.request.TopicCategoryMarkRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * <p>
 * 专题表 服务实现类
 * </p>
 *
 * @author maihehe
 * @since 2025-05-21
 */
@Slf4j
@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements ITopicService {
    @Autowired private TopicMapper topicMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markCategory(List<TopicCategoryMarkRequest> requests) {
        for (TopicCategoryMarkRequest req : requests) {
            String topicPath = req.getTopicPath();
            String category = (req.getCategory() == null || req.getCategory().isBlank()) ? null : req.getCategory();

            int rows = topicMapper.update(
                    null,
                    Wrappers.<Topic>lambdaUpdate()
                            .eq(Topic::getPath, topicPath)
                            .set(Topic::getCategory, category)
            );
            if (rows == 0) {
                // 建议保证 path 唯一；这里明确报哪个 path 没更新到
                throw new RuntimeException("更新失败，未找到对应的 Topic，path=" + topicPath);
            }
        }
    }

    @Override
    public List<Topic> loadTopics() {
        // 查询表中所有 Topic
        return this.list();
    }
    @Override
    public List<Topic> loadTopicByAZ() {
        // 按 name 字母升序排列
        List<Topic> list = this.lambdaQuery().list();
        list.sort(Comparator.comparing(Topic::getName));
//        list.forEach(t -> log.info("Topic: {}", t));
        return list;
    }

    @Override
    public List<TopicCategoryDTO> loadTopicCategory() {
        return topicMapper.selectAllTopicCategoryDTOs();
    }
}
