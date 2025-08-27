package com.maihehe.blogcore._01_config.Sentinel;


import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.SentinelWebInterceptor;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.config.SentinelWebMvcConfig;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Configuration
public class SentinelConfig implements WebMvcConfigurer {

    // —— 规则文件位置（生产建议用外部配置传入或绝对路径）——
    private static final String RULE_DIR  = "./config/Sentinel"; // 基准目录
    private static final String FLOW_FILE = RULE_DIR + "/flow.json";
    private static final String DEGR_FILE = RULE_DIR + "/degrade.json";

    // 文件变更轮询周期（毫秒）
    private static final long REFRESH_MS = 2000L;

    // ========== 限流/降级被拦截时的统一返回（429 JSON） ==========
    @Bean
    public BlockExceptionHandler sentinelBlockExceptionHandler() {
        return (request, response, resourceName, e) -> {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"blocked\",\"message\":\"too many requests\"}");
        };
    }

    // ========== Sentinel WebMVC 拦截器（v6x，适配 Spring 6 / Boot 3） ==========
    @Bean
    public SentinelWebInterceptor sentinelWebInterceptor(BlockExceptionHandler handler) {
        SentinelWebMvcConfig cfg = new SentinelWebMvcConfig();
        // 资源名是否包含 HTTP 方法；这里禁用 → 资源名就是纯 URL 路径
        cfg.setHttpMethodSpecify(false);
        cfg.setBlockExceptionHandler(handler);
        return new SentinelWebInterceptor(cfg);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 这里直接调用 @Bean 方法即可（@Configuration 代理会返回同一 Bean 实例）
        registry.addInterceptor(sentinelWebInterceptor(sentinelBlockExceptionHandler()))
                .addPathPatterns("/**");
    }

    // ========== 仅本地文件“只读 + 热更新” ==========
    @PostConstruct
    public void initFileDataSources() throws Exception {
        ensureFilesWithDefaults();

        ObjectMapper om = new ObjectMapper();

        // --- 解析器：把受检异常转成 RuntimeException ---
        com.alibaba.csp.sentinel.datasource.Converter<String, List<FlowRule>> flowParser = src -> {
            try {
                return om.readValue(src, new com.fasterxml.jackson.core.type.TypeReference<List<FlowRule>>() {});
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException("Failed to parse flow rules JSON", e);
            }
        };

        com.alibaba.csp.sentinel.datasource.Converter<String, List<DegradeRule>> degrParser = src -> {
            try {
                return om.readValue(src, new com.fasterxml.jackson.core.type.TypeReference<List<DegradeRule>>() {});
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException("Failed to parse degrade rules JSON", e);
            }
        };

        // --- 只读 + 热更新（用最简单 2 参构造）---
        com.alibaba.csp.sentinel.datasource.ReadableDataSource<String, List<FlowRule>> flowDs =
                new com.alibaba.csp.sentinel.datasource.FileRefreshableDataSource<>(
                        FLOW_FILE, flowParser
                );
        com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager.register2Property(flowDs.getProperty());

        com.alibaba.csp.sentinel.datasource.ReadableDataSource<String, List<DegradeRule>> degrDs =
                new com.alibaba.csp.sentinel.datasource.FileRefreshableDataSource<>(
                        DEGR_FILE, degrParser
                );
        com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager.register2Property(degrDs.getProperty());
    }

    // 初次启动时创建目录与默认规则（可按需修改）
    private static void ensureFilesWithDefaults() throws IOException {
        File dir = new File(RULE_DIR);
        if (!dir.exists()) dir.mkdirs();

        File flow = new File(FLOW_FILE);
        if (!flow.exists()) {
            String flowJson = """
            [
              {"resource":"/api/oss/figureUrl","grade":1,"count":20,"controlBehavior":0,"clusterMode":false},
              {"resource":"/api/admin/oss/figuresScan","grade":0,"count":1,"clusterMode":false}
            ]
            """;
            Files.writeString(flow.toPath(), flowJson);
        }

        File degr = new File(DEGR_FILE);
        if (!degr.exists()) {
            String degrJson = """
            [
              {"resource":"/api/admin/oss/figuresScan","grade":0,"count":1000,
               "timeWindow":30,"minRequestAmount":10,"statIntervalMs":60000}
            ]
            """;
            Files.writeString(degr.toPath(), degrJson);
        }
    }
}

