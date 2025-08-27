//package com.maihehe.blogcore._01_config.SpringSecurity;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import java.util.List;
//
//@Configuration
//public class CorsConfig {
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration c = new CorsConfiguration();
//        // ✅ 精确列出前端页面的 Origin（带协议）
//        c.setAllowedOrigins(List.of(
//                "http://localhost:5173",
//                "http://127.0.0.1:5173"
//                // 上线时再加你的正式前端域名，例如：
//                // "https://your.site"
//        ));
//        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
//        c.setAllowedHeaders(List.of("*"));
//        c.setAllowCredentials(true);
//
//        // 若你有内网 IP/主机名等不固定来源，可用通配：
//        // c.setAllowedOriginPatterns(List.of("http://192.168.*:*", "http://*.local:*", "https://*.your.site"));
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/api/**", c); // 只给 /api/** 生效
//        return source;
//    }
//}
