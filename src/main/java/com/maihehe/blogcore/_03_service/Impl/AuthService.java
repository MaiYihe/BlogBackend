package com.maihehe.blogcore._03_service.Impl;

import com.maihehe.blogcore._01_config.SpringSecurity.DBUserDetailsManager;
import com.maihehe.blogcore._01_config.SpringSecurity.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private DBUserDetailsManager userDetailsManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    //    用于控制注册的 API 是否开启，默认值是 false
    @Value("${auth.register-enabled:false}")
    private boolean registerEnabled;

    /**
     * 注册
     * @param username
     * @param password
     * @return
     */
    public String register(String username, String password) {
        log.info("注册新用户: {}", username);
        if (!registerEnabled) {
            throw new IllegalStateException("注册功能已关闭");
        }

        if (userDetailsManager.userExists(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 构造 UserDetails 并插入数据库
        UserDetails user = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("ADMIN")
                .build();

        userDetailsManager.createUser(user);

        // 注册后立即登陆
        return login(username, password);
    }

    /**
     * 登陆
     * @param username
     * @param password
     * @return
     */
    public String login(String username, String password) {
        log.info("用户 {} 正在登录...", username);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return jwtUtils.generateToken(userDetails);
    }

}
