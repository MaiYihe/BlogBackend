package com.maihehe.blogcore;

import com.aliyun.oss.*;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@SpringBootTest
class BlogCoreApplicationTests {
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.bucket}")
    private String bucket;

    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;

    @Test
    void contextLoads() {
        String objectName = "test/demo.txt";
        String filePath   = "/home/maiyihe/Documents/6_Interest-drivenProject/Blog/maiyiheBlogCore/blogCore/data/Backend/-1_figures/Pasted image 20250208123001.png";

        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        // ⚠️ 关键：不要启用 V4
        // conf.setSignatureVersion(SignVersion.V4);
        conf.setConnectionTimeout(10_000);
        conf.setSocketTimeout(20_000);
        conf.setMaxErrorRetry(1);

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, conf);

            // 可选：确认 bucket 地域与 endpoint 一致（应打印 cn-chengdu）
            String loc = ossClient.getBucketLocation(bucket);
            System.out.println("Bucket Location = " + loc);

            PutObjectRequest req = new PutObjectRequest(bucket, objectName, new File(filePath));
            PutObjectResult res  = ossClient.putObject(req);

            System.out.println("上传成功, key=" + objectName + ", etag=" + res.getETag());
        } catch (OSSException oe) {
            System.err.println("OSSException: " + oe.getErrorMessage() + ", Code=" + oe.getErrorCode());
        } catch (ClientException ce) {
            System.err.println("ClientException: " + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}