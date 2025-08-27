package com.maihehe.blogcore._04_mapper;

import com.maihehe.blogcore._05_entity.Topic;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maihehe.blogcore._06_DTO.normalDTO.TopicCategoryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 专题表 Mapper 接口
 * </p>
 *
 * @author maihehe
 * @since 2025-05-21
 */
@Mapper
public interface TopicMapper extends BaseMapper<Topic> {
    void batchInsert(@Param("topics") List<Topic> topics);
    void deleteByPaths(@Param("topics") List<Topic> topics);

    List<TopicCategoryDTO> selectAllTopicCategoryDTOs();
}
