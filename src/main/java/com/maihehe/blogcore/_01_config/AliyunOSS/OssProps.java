package com.maihehe.blogcore._01_config.AliyunOSS;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProps {
    private String endpoint;
    private String bucket;
    private String accessKeyId;
    private String accessKeySecret;
}