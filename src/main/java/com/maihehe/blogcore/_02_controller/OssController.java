package com.maihehe.blogcore._02_controller;

import com.maihehe.blogcore._03_service.Impl.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/")
public class OssController {
    @Autowired
    OssService ossService;

    @PostMapping("/admin/oss/figuresScan")
    public ResponseEntity<Map<String, Object>> scanFiguresToOSS() {
        log.info("===========尝试扫描图片并上传到 OSS");
        long t0 = System.currentTimeMillis();
        try {
            ossService.figuresScan();
            long ms = System.currentTimeMillis() - t0;
            log.info("===========扫描/上传完成，耗时 {} ms", ms);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "message", "figures scanned and uploaded",
                    "durationMs", ms
            ));
        } catch (Exception e) {
            long ms = System.currentTimeMillis() - t0;
            log.error("===========扫描/上传失败，耗时 {} ms", ms, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage() != null ? e.getMessage() : "internal error",
                    "durationMs", ms
            ));
        }
    }

    /**
     * 页面显示的时候，图片加载
     * @param objectKey
     * @return
     */
    @GetMapping("/oss/figureUrl")
    public ResponseEntity<Map<String, Object>> figureUrl(@RequestParam("objectKey") String objectKey) {
        log.info("===========尝试加载图片：{}", objectKey);
        try{
            // 不传 downloadFilename，避免浏览器强制下载
            String url = ossService.presignDownload(objectKey,null);
            log.info("=============图片加载成功");
            return ResponseEntity.ok(Map.of(
                    "url", url,   // 注意这里要用 toString()
                    "expiresAt", Instant.now().plus(10, ChronoUnit.MINUTES).toString()
            ));
        } catch(Exception e){
            log.error("加载图片失败", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to sign url"));
        }
    }
}
