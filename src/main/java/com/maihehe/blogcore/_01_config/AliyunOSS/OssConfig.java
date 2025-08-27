package com.maihehe.blogcore._01_config.AliyunOSS;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(OssProps.class)
public class OssConfig {

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient(OssProps props) {
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        // ⚠️ 不要开启 V4（除非你用的是支持 .region(...) 的新链式 Builder）
        // conf.setSignatureVersion(SignVersion.V4);
        conf.setConnectionTimeout(10_000);
        conf.setSocketTimeout(20_000);
        conf.setMaxErrorRetry(1);

        // 直接用老的 build(...) 重载
        return new OSSClientBuilder().build(
                props.getEndpoint(),
                props.getAccessKeyId(),
                props.getAccessKeySecret(),
                conf
        );
    }
}


