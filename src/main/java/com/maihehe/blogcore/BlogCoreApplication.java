package com.maihehe.blogcore;

import com.maihehe.blogcore._01_config.ContentScan.NoteScanConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
@EnableConfigurationProperties(NoteScanConfig.class) // 扫描配置类
public class BlogCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogCoreApplication.class, args);
    }
}
