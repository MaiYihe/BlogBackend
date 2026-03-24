package com.maihehe.blogcore._02_controller;

import com.maihehe.blogcore._03_service.Impl.RagChunkService;
import com.maihehe.blogcore._03_service.Impl.RagChatService;
import com.maihehe.blogcore._03_service.Impl.RagRateLimitService;
import com.maihehe.blogcore._03_service.Impl.RagIngestService;
import com.maihehe.blogcore._03_service.Impl.RagJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/admin/rag")
public class RagController {
    @Autowired
    private RagChunkService ragChunkService;
    @Autowired
    private RagIngestService ragIngestService;
    @Autowired
    private RagJobService ragJobService;
    @Autowired
    private RagChatService ragChatService;
    @Autowired
    private RagRateLimitService ragRateLimitService;

    @PostMapping("/chunk")
    public ResponseEntity<Map<String, Object>> chunk(
            @RequestParam(value = "output", required = false) String output,
            @RequestParam(value = "maxChars", defaultValue = "2000") int maxChars,
            @RequestParam(value = "overlap", defaultValue = "200") int overlap,
            @RequestParam(value = "ext", required = false) List<String> exts
    ) {
        log.info("开始 RAG 切分: output={}, maxChars={}, overlap={}, exts={}", output, maxChars, overlap, exts);
        try {
            RagChunkService.Result result = ragChunkService.runChunking(output, maxChars, overlap, exts);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "fileCount", result.fileCount(),
                    "chunkCount", result.chunkCount(),
                    "output", result.outputPath(),
                    "rawOutput", result.rawOutput()
            ));
        } catch (Exception e) {
            log.error("RAG 切分失败", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingest(
            @RequestParam(value = "input", required = false) String input,
            @RequestParam(value = "collection", defaultValue = "notes") String collection,
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "dim", required = false) Integer dim,
            @RequestParam(value = "batchSize", defaultValue = "64") int batchSize,
            @RequestParam(value = "normalize", defaultValue = "true") boolean normalize,
            @RequestParam(value = "storeText", defaultValue = "true") boolean storeText,
            @RequestParam(value = "recreate", defaultValue = "false") boolean recreate,
            @RequestParam(value = "logEvery", required = false) Integer logEvery,
            @RequestParam(value = "qdrantUrl", required = false) String qdrantUrl
    ) {
        log.info("开始 RAG 入库: input={}, collection={}, provider={}, model={}", input, collection, provider, model);
        try {
            RagIngestService.Result result = ragIngestService.runIngest(
                    input, collection, provider, model, dim, batchSize, normalize, storeText, recreate, logEvery, qdrantUrl
            );
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "upserted", result.upserted(),
                    "collection", result.collection(),
                    "qdrantUrl", result.qdrantUrl(),
                    "rawOutput", result.rawOutput()
            ));
        } catch (Exception e) {
            log.error("RAG 入库失败", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> run(
            @RequestParam(value = "output", required = false) String output,
            @RequestParam(value = "maxChars", defaultValue = "2000") int maxChars,
            @RequestParam(value = "overlap", defaultValue = "200") int overlap,
            @RequestParam(value = "ext", required = false) List<String> exts,
            @RequestParam(value = "input", required = false) String input,
            @RequestParam(value = "collection", defaultValue = "notes") String collection,
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "dim", required = false) Integer dim,
            @RequestParam(value = "batchSize", defaultValue = "64") int batchSize,
            @RequestParam(value = "normalize", defaultValue = "true") boolean normalize,
            @RequestParam(value = "storeText", defaultValue = "true") boolean storeText,
            @RequestParam(value = "recreate", defaultValue = "false") boolean recreate,
            @RequestParam(value = "logEvery", required = false) Integer logEvery,
            @RequestParam(value = "qdrantUrl", required = false) String qdrantUrl,
            @RequestParam(value = "async", defaultValue = "true") boolean async,
            @RequestParam(value = "diffFirst", defaultValue = "true") boolean diffFirst,
            @RequestParam(value = "pruneDeleted", defaultValue = "false") boolean pruneDeleted,
            @RequestParam(value = "pruneBatchSize", required = false) Integer pruneBatchSize,
            @RequestParam(value = "pruneAllowEmpty", defaultValue = "false") boolean pruneAllowEmpty
    ) {
        log.info("RAG run requested: chunk+ingest, async={}, diffFirst={}, collection={}", async, diffFirst, collection);
        RagJobService.RagPipelineRequest req = new RagJobService.RagPipelineRequest(
                output, maxChars, overlap, exts, input, collection, provider, model, dim, batchSize,
                normalize, storeText, recreate, logEvery, qdrantUrl,
                diffFirst,
                pruneDeleted, pruneBatchSize, pruneAllowEmpty
        );
        if (async) {
            String jobId = ragJobService.startAsync(req);
            return ResponseEntity.accepted().body(Map.of(
                    "status", "accepted",
                    "jobId", jobId
            ));
        }
        try {
            long t0 = System.currentTimeMillis();
            RagChunkService.ResolvedPaths rp = ragChunkService.collectPaths();
            RagChunkService.PipelineResult pipelineRes = ragChunkService.runPipeline(
                    rp,
                    output,
                    input,
                    maxChars,
                    overlap,
                    exts,
                    collection,
                    provider,
                    model,
                    dim,
                    batchSize,
                    normalize,
                    storeText,
                    recreate,
                    logEvery,
                    qdrantUrl,
                    diffFirst,
                    pruneDeleted,
                    pruneBatchSize,
                    pruneAllowEmpty
            );
            long durationMs = System.currentTimeMillis() - t0;
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "durationMs", durationMs,
                    "chunk", Map.of(
                            "fileCount", pipelineRes.chunkResult().fileCount(),
                            "chunkCount", pipelineRes.chunkResult().chunkCount(),
                            "output", pipelineRes.chunkResult().outputPath()
                    ),
                    "ingest", Map.of(
                            "upserted", pipelineRes.ingestResult().upserted(),
                            "collection", pipelineRes.ingestResult().collection(),
                            "qdrantUrl", pipelineRes.ingestResult().qdrantUrl()
                    ),
                    "diff", pipelineRes.diffResult() == null ? null : Map.of(
                            "currentDocs", pipelineRes.diffResult().currentDocs(),
                            "existingDocs", pipelineRes.diffResult().existingDocs(),
                            "addedDocs", pipelineRes.diffResult().addedDocs(),
                            "changedDocs", pipelineRes.diffResult().changedDocs(),
                            "deletedDocs", pipelineRes.diffResult().deletedDocs(),
                            "missingFiles", pipelineRes.diffResult().missingFiles()
                    ),
                    "prune", pipelineRes.pruneResult() == null ? null : Map.of(
                            "deletedDocs", pipelineRes.pruneResult().deletedDocs(),
                            "currentDocs", pipelineRes.pruneResult().currentDocs(),
                            "existingDocs", pipelineRes.pruneResult().existingDocs()
                    )
            ));
        } catch (Exception e) {
            log.error("RAG pipeline failed", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/chat")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> chat(@RequestBody Map<String, Object> body,
                                                                       HttpServletRequest request) {
        try {
            String sessionId = body == null ? null : String.valueOf(body.getOrDefault("session_id", ""));
            String clientIp = resolveClientIp(request);
            String username = resolveUserId();
            String userId = "[" + clientIp + "," + username + "]";
            RagRateLimitService.LimitDecision decision = ragRateLimitService.check(userId);
            if (!decision.allowed()) {
                return CompletableFuture.completedFuture(
                        ResponseEntity.status(429).body(Map.of(
                                "status", "rate_limited",
                                "message", "rate limit exceeded",
                                "reason", decision.reason(),
                                "limit", decision.limit(),
                                "userId", userId
                        ))
                );
            }
            return ragChatService.chatAsync(body, sessionId, userId, clientIp)
                    .thenApply(ResponseEntity::ok)
                    .exceptionally(ex -> {
                        log.error("RAG chat failed", ex);
                        return ResponseEntity.status(500).body(Map.of(
                                "status", "error",
                                "message", ex.getMessage()
                        ));
                    });
        } catch (Exception e) {
            log.error("RAG chat failed", e);
            return CompletableFuture.completedFuture(ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            )));
        }
    }

    @GetMapping(value = "/chat/stream/{taskId}", produces = "text/event-stream")
    public SseEmitter chatStream(@PathVariable("taskId") String taskId,
                                 HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        String username = resolveUserId();
        String userId = "[" + clientIp + "," + username + "]";
        return ragChatService.streamTask(taskId, userId, clientIp);
    }

    @PostMapping("/chat/cancel/{taskId}")
    public ResponseEntity<Map<String, Object>> chatCancel(@PathVariable("taskId") String taskId) {
        try {
            Map<String, Object> res = ragChatService.cancelTask(taskId);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("RAG cancel failed", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    private String resolveUserId() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return "guest";
            }
            Object principal = auth.getPrincipal();
            if (principal == null) {
                return "guest";
            }
            if ("anonymousUser".equals(principal)) {
                return "guest";
            }
            return auth.getName();
        } catch (Exception e) {
            return "guest";
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            if (parts.length > 0) {
                return parts[0].trim();
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<Map<String, Object>> job(@PathVariable("jobId") String jobId) {
        RagJobService.RagJob job = ragJobService.getJob(jobId);
        if (job == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "not_found",
                    "jobId", jobId
            ));
        }
        Long durationMs = null;
        if (job.getStartedAt() != null && job.getFinishedAt() != null) {
            durationMs = java.time.Duration.between(job.getStartedAt(), job.getFinishedAt()).toMillis();
        }
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("status", job.getStatus().name());
        body.put("jobId", job.getId());
        body.put("startedAt", job.getStartedAt());
        body.put("finishedAt", job.getFinishedAt());
        body.put("durationMs", durationMs);
        body.put("error", job.getError());

        if (job.getChunkResult() == null) {
            body.put("chunk", null);
        } else {
            Map<String, Object> chunk = new java.util.LinkedHashMap<>();
            chunk.put("fileCount", job.getChunkResult().fileCount());
            chunk.put("chunkCount", job.getChunkResult().chunkCount());
            chunk.put("output", job.getChunkResult().outputPath());
            body.put("chunk", chunk);
        }

        if (job.getIngestResult() == null) {
            body.put("ingest", null);
        } else {
            Map<String, Object> ingest = new java.util.LinkedHashMap<>();
            ingest.put("upserted", job.getIngestResult().upserted());
            ingest.put("collection", job.getIngestResult().collection());
            ingest.put("qdrantUrl", job.getIngestResult().qdrantUrl());
            body.put("ingest", ingest);
        }

        if (job.getDiffResult() == null) {
            body.put("diff", null);
        } else {
            Map<String, Object> diff = new java.util.LinkedHashMap<>();
            diff.put("currentDocs", job.getDiffResult().currentDocs());
            diff.put("existingDocs", job.getDiffResult().existingDocs());
            diff.put("addedDocs", job.getDiffResult().addedDocs());
            diff.put("changedDocs", job.getDiffResult().changedDocs());
            diff.put("deletedDocs", job.getDiffResult().deletedDocs());
            diff.put("missingFiles", job.getDiffResult().missingFiles());
            body.put("diff", diff);
        }

        if (job.getPruneResult() == null) {
            body.put("prune", null);
        } else {
            Map<String, Object> prune = new java.util.LinkedHashMap<>();
            prune.put("deletedDocs", job.getPruneResult().deletedDocs());
            prune.put("currentDocs", job.getPruneResult().currentDocs());
            prune.put("existingDocs", job.getPruneResult().existingDocs());
            body.put("prune", prune);
        }

        return ResponseEntity.ok(body);
    }
}
