package com.maihehe.blogcore._03_service;

import com.maihehe.blogcore._05_entity.Topic;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maihehe.blogcore._06_DTO.normalDTO.TopicCategoryDTO;
import com.maihehe.blogcore._06_DTO.request.TopicCategoryMarkRequest;

import java.util.List;

/**
 * <p>
 * 专题表 服务类
 * </p>
 *
 * @author maihehe
 * @since 2025-05-21
 */
public interface ITopicService extends IService<Topic> {
    List<Topic> loadTopics();
    List<Topic> loadTopicByAZ();

    List<TopicCategoryDTO> loadTopicCategory();
    void markCategory(List<TopicCategoryMarkRequest> requests);
}
