package com.maihehe.blogcore._04_mapper;

import com.maihehe.blogcore._05_entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;


/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author maihehe
 * @since 2025-05-19
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}

