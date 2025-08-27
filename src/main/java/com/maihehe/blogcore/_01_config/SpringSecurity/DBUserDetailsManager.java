package com.maihehe.blogcore._01_config.SpringSecurity;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maihehe.blogcore._05_entity.User;
import com.maihehe.blogcore._04_mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Component;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.List;

@Component
@Slf4j
public class DBUserDetailsManager implements UserDetailsManager, UserDetailsPasswordService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserQueryService userQueryService;

    @Override
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        return null;
    }
    /**
     * 向数据库中插入新的用户信息
     * @param user
     */
    @Override
    public void createUser(UserDetails user) {
        User tmpUser = new User();
        tmpUser.setUsername(user.getUsername());
        tmpUser.setPassword(user.getPassword());

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority) // 提取如 "ROLE_USER"
                .collect(Collectors.toList());
        tmpUser.setRoles(String.join(",", roles));
        userMapper.insert(tmpUser);
    }

    @Override
    public void updateUser(UserDetails user) {
    }

    @Override
    public void deleteUser(String username) {
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
    }

    @Override
    public boolean userExists(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        return userMapper.selectCount(wrapper) > 0;
    }

    /**
     * 通过用户名，从数据库中获取用户信息
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("解析出的JWT中的 user 是： " + username);


        User user = userQueryService.getUserByUsername(username);
        // user.getRoles() 是一个 String，比如 "ROLE_ADMIN"
        List<SimpleGrantedAuthority> authorities = Arrays.stream(user.getRoles().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .toList();

        log.info("用户 [{}] 的权限: {}", user.getUsername(),
                authorities.stream().map(GrantedAuthority::getAuthority).toList());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }

}
