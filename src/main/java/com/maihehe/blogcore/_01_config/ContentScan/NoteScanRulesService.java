package com.maihehe.blogcore._01_config.ContentScan;

// NoteScanRulesService.java
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Slf4j
@Service
public class NoteScanRulesService {
    private final NoteScanConfig rules;

    public NoteScanRulesService(NoteScanConfig rules) {
        this.rules = rules; // Spring 注入已绑定好的配置
        // 构造时打印配置，方便确认注入是否成功
        log.info("NoteScanRulesService initialized with config: skipNames={}, skipPrefixes={}, skipSuffixes={}",
                rules.getSkipNames(), rules.getSkipPrefixes(), rules.getSkipSuffixes());
    }
    public boolean shouldSkip(Path path) {
        String fileName = path.getFileName().toString();
        if (rules.getSkipNames().contains(fileName)) {
            log.info("文件 {} 跳过成功",path.getFileName());
            return true;
        }
        for (String p : rules.getSkipPrefixes()) {
            if (!p.isEmpty() && fileName.startsWith(p)){
                log.info("文件 {} 跳过成功",path.getFileName());
                return true;
            }
        }
        for (String s : rules.getSkipSuffixes()) {
            if (!s.isEmpty() && fileName.endsWith(s)){
                log.info("文件 {} 跳过成功",path.getFileName());
                return true;
            }
        }
        return false;
    }
}