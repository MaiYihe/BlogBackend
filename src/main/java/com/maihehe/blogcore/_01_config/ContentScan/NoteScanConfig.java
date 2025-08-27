package com.maihehe.blogcore._01_config.ContentScan;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;


@Data
//告诉 Spring Boot：把配置文件里以 contentScan 开头的字段，映射到这个类的属性上
@ConfigurationProperties(prefix = "content-scan") // 前缀必须是“规范格式”（kebab-case）
public class NoteScanConfig {
    private List<String> skipNames = new ArrayList<>();
    private List<String> skipPrefixes = new ArrayList<>();
    private List<String> skipSuffixes = new ArrayList<>();
}
