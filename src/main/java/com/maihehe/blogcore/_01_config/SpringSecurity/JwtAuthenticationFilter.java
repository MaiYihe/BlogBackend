package com.maihehe.blogcore._01_config.SpringSecurity;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private DBUserDetailsManager userDetailsService; // 自己实现的 UserDetailsService

    // 流程：拿 token → 解出用户 → 包装成身份认证对象 → 通知 Spring Security：这个人已经登录了
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("进入JWT认证过滤器...");
        // 从请求头获取 token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("访问没携带 token, 跳过JWT认证过滤器...");
            filterChain.doFilter(request, response); // 没 token 就放行
            return;
        }

        // 从第 7 个字符开始截取（即跳过 "Bearer " 这 7 个字符），从 Authorization 请求头中提取出 JWT 的 token 字符串部分；
        String token = authHeader.substring(7);
        String username = jwtUtils.extractUsername(token);

        // 如果用户已通过 token 识别出用户名，并且当前请求上下文中还没有认证信息，则进行认证处理
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 防御性编程，传入 userDetails 是担心查询出来的 username 和获取 userdetails 的 username 不一致(错误的自定义)
            // 判断传入的 JWT token 是否有效，并且与当前 userDetails 匹配
            if (jwtUtils.isTokenValid(token, userDetails)) {
                // 创建一个已认证的身份凭证，null 表示密码（这里不再需要了）
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );

                // 把一些来自 HttpServletRequest 的细节信息加进去
                // 比如：IP 地址、Session ID、代理信息
                // 这一步是可选的，但在登录日志审计、安全分析时会很有用
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                // 设置上下文认证信息
                // 之后在任何接口里，只要加了：Authentication auth = SecurityContextHolder.getContext().getAuthentication(); 就可以随时取出当前登录用户的信息了（前提是这个请求中有 token）
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

}
