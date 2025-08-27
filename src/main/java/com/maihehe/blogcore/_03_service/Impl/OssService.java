package com.maihehe.blogcore._03_service.Impl;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.maihehe.blogcore._01_config.AliyunOSS.OssProps;
import com.maihehe.blogcore._03_service.Impl.ContentService.PathReader;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class OssService {
    @Autowired private OSS oss;
    @Autowired private OssProps props;
    @Autowired private PathReader pathReader;

    private static final long LIMIT_BYTES = 100 * 1024; // 100KB
    private static final Set<String> IMAGE_SUFFIX = Set.of("jpg","jpeg","png","webp","bmp","gif");

    /**
     * 扫描本地磁盘，Topic 下所有的 -1_figures 里面的文件，上传到 OSS
     */
    @Transactional
    public void figuresScan() {
        /** 第二步，将图片上传到 OSS**/
        log.info("开始上传图片到 OSS");
        List<Path> figurePaths = pathReader.readTopicFigures();

        // 计算文件大小
        long totalSize = figurePaths.stream()
                .map(Path::toFile)
                .filter(File::isFile)
                .mapToLong(File::length)
                .sum();
        log.info("将上传 {} 个文件，总大小 {} 字节 (~{} KB, ~{} MB)",
                figurePaths.size(),
                totalSize,
                totalSize / 1024,
                totalSize / (1024 * 1024));


//        // 取一个文件上传测试效果
//        String targetName = "Pasted image 20241114192136.png";
//        Optional<Path> match = figurePaths.stream()
//                .filter(p -> p.getFileName().toString().equals(targetName))
//                .findFirst();
//        if (match.isPresent()) {
//            Path path = match.get();
//            File file = path.toFile();
//            String key = path.getFileName().toString();
//            log.info("准备上传 key={}, size={}B", key, file.length());
//            OssService.Result result = ossService.uploadFile(key, file);
//            log.info("上传完成：key={}, etag={}, url={}",
//                    result.getObjectKey(), result.getETag(), result.getUrl());
//        } else {
//            log.warn("没有找到目标文件: {}", targetName);
//        }

        // 正式开始上传，所有文件
        for (Path path : figurePaths) {
            String fileName = path.getFileName().toString();
            File file = path.toFile();
            OssService.Result result = uploadFile(fileName,file);
            log.info("上传完成：key={}, etag={}, url={}",
                    result.getObjectKey(), result.getETag(), result.getUrl());
        }
        log.info("上传到 OSS 成功");
    }


    /** 单个文件上传 **/
    /** 对外：自动压缩到 ≤100KB 再上传（必要时从 png/webp 转 jpg） */
    private Result uploadFile(String objectKey, File file) {
        String ext = extOf(objectKey).toLowerCase();
        boolean isImage = IMAGE_SUFFIX.contains(ext);

        try {
            if (!isImage || file.length() <= LIMIT_BYTES) {
                // 非图片或 ≤100KB：原样上传
                ObjectMetadata meta = new ObjectMetadata();
                String contentType = probeContentType(file.toPath());
                if (contentType != null) meta.setContentType(contentType);
                PutObjectRequest req = new PutObjectRequest(props.getBucket(), objectKey, file);
                req.setMetadata(meta);
                var r = oss.putObject(req);
                return new Result(objectKey, r.getETag(), buildPublicUrl(objectKey));
            }

            // 图片且 >100KB：按“原后缀”压缩（JPG：降质+缩；PNG/GIF/WebP/BMP：仅缩）
            CompressOut out = compressPreserveExt(file, ext, LIMIT_BYTES);

            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(out.bytes.length);
            meta.setContentType(out.contentType);

            try (ByteArrayInputStream in = new ByteArrayInputStream(out.bytes)) {
                var r = oss.putObject(props.getBucket(), objectKey, in, meta); // ★ key 不变
                return new Result(objectKey, r.getETag(), buildPublicUrl(objectKey));
            }
        } catch (Exception e) {
            log.error("uploadFile failed, key={}", objectKey, e);
            throw new RuntimeException(e);
        }
    }

    // ===== 预签名（保持你原逻辑） =====
    @Cacheable(cacheNames = "ossPresignDownload", key = "#p0")
    public String presignDownload(String objectKey, String downloadFilename) {
        Duration ttl = Duration.ofMinutes(8);
        Date expiration = new Date(System.currentTimeMillis() + ttl.toMillis());
        GeneratePresignedUrlRequest req =
                new GeneratePresignedUrlRequest(props.getBucket(), objectKey, HttpMethod.GET);
        req.setExpiration(expiration);
        if (downloadFilename != null && !downloadFilename.isEmpty()) {
            String encoded = URLEncoder.encode(downloadFilename, StandardCharsets.UTF_8);
            req.addQueryParameter("response-content-disposition",
                    "attachment; filename*=UTF-8''" + encoded);
        }
        return oss.generatePresignedUrl(req).toString();
    }



    // ===== 压缩核心 =====
    private static class CompressOut { byte[] bytes; String contentType; }

    private static CompressOut compressPreserveExt(File src, String ext, long limit) throws IOException {
        BufferedImage img = ImageIO.read(src);
        if (img == null) throw new IOException("Unsupported image: " + src);

        int w = img.getWidth(), h = img.getHeight();
        final int MAX_TRY = 15;

        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            float q = 0.90f;
            for (int i = 0; i < MAX_TRY; i++) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(128 * 1024);
                Thumbnails.of(img).size(w, h).outputFormat("jpg").outputQuality(q).toOutputStream(baos);
                if (baos.size() <= limit) {
                    CompressOut out = new CompressOut();
                    out.bytes = baos.toByteArray();
                    out.contentType = "image/jpeg";
                    return out;
                }
                if (q > 0.28f) q -= 0.15f; else { w = Math.max(1, (int)(w * 0.9)); h = Math.max(1, (int)(h * 0.9)); }
            }
            // 兜底
            ByteArrayOutputStream baos = new ByteArrayOutputStream(128 * 1024);
            Thumbnails.of(img).size(w, h).outputFormat("jpg").outputQuality(0.25f).toOutputStream(baos);
            CompressOut out = new CompressOut();
            out.bytes = baos.toByteArray(); out.contentType = "image/jpeg";
            return out;
        } else {
            // PNG / GIF / WEBP / BMP：保持原格式，仅缩尺寸
            String format = ext; // Thumbnailator 需要 formatName
            String contentType = switch (ext) {
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                case "webp" -> "image/webp";
                case "bmp" -> "image/bmp";
                default -> "application/octet-stream";
            };

            for (int i = 0; i < MAX_TRY; i++) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(128 * 1024);
                // 注意：这里不设置 outputQuality（PNG/GIF 等质量参数基本不起作用）
                Thumbnails.of(img).size(w, h).outputFormat(format).toOutputStream(baos);
                if (baos.size() <= limit) {
                    CompressOut out = new CompressOut();
                    out.bytes = baos.toByteArray();
                    out.contentType = contentType;
                    return out;
                }
                // 继续缩小分辨率
                w = Math.max(1, (int)(w * 0.9));
                h = Math.max(1, (int)(h * 0.9));
            }
            // 兜底（最小尺寸）
            ByteArrayOutputStream baos = new ByteArrayOutputStream(128 * 1024);
            Thumbnails.of(img).size(w, h).outputFormat(format).toOutputStream(baos);
            CompressOut out = new CompressOut();
            out.bytes = baos.toByteArray(); out.contentType = contentType;
            return out;
        }
    }

    // ===== 小工具 =====
    private static String extOf(String name) { int i = name.lastIndexOf('.'); return i>=0 ? name.substring(i+1) : ""; }
    private static String changeExt(String name, String newExt) { int i = name.lastIndexOf('.'); return (i>=0?name.substring(0,i):name) + "." + newExt; }

    private static String probeContentType(Path path) {
        try {
            String t = Files.probeContentType(path);
            return (t != null) ? t : guessFromName(path.getFileName().toString());
        } catch (Exception ignore) {
            return guessFromName(path.getFileName().toString());
        }
    }
    private static String guessFromName(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
    private String buildPublicUrl(String objectKey) {
        String host = props.getEndpoint().replace("https://", "").replace("http://", "");
        return "https://" + props.getBucket() + "." + host + "/" + objectKey;
    }

    /** 极简返回值 */
    public static class Result {
        private final String objectKey; private final String eTag; private final String url;
        public Result(String objectKey, String eTag, String url) { this.objectKey = objectKey; this.eTag = eTag; this.url = url; }
        public String getObjectKey() { return objectKey; } public String getETag() { return eTag; } public String getUrl() { return url; }
    }
}
