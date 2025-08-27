package com.maihehe.blogcore._01_config.SpringSecurity;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maihehe.blogcore._05_entity.User;
import com.maihehe.blogcore._04_mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 结合 DBUserDetailsService，用 SpringCache 创建缓存的类
 */
@Slf4j
@Service
public class UserQueryService {
    @Autowired
    UserMapper userMapper;
    @Cacheable(value = "userCache", key = "#p0")
    public com.maihehe.blogcore._05_entity.User getUserByUsername(String username) {
        log.info("当前 user 无缓存，将从数据库中查找");
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return userMapper.selectOne(queryWrapper);
    }
}
