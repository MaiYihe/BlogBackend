package com.maihehe.blogcore._03_service.Impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maihehe.blogcore._04_mapper.NoteMapper;
import com.maihehe.blogcore._05_entity.Note;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RagChunkService {
    private final NoteMapper noteMapper;

    @Value("${rag.python:python3}")
    private String pythonCmd;

    @Value("${rag.python.url:}")
    private String ragPythonUrl;

    @Value("${rag.data.root:data}")
    private String ragDataRoot;

    public RagChunkService(NoteMapper noteMapper) {
        this.noteMapper = noteMapper;
    }

    public record ResolvedPaths(List<String> paths, int totalNotes, String root) {}

    public record PipelineResult(
            RagChunkService.Result chunkResult,
            RagIngestService.Result ingestResult,
            RagIngestService.DiffResult diffResult,
            RagIngestService.PruneResult pruneResult
    ) {}

    public ResolvedPaths collectPaths() {
        List<Note> notes = noteMapper.selectList(
                Wrappers.<Note>lambdaQuery()
                        .select(Note::getCurrentPath, Note::getIsFolder, Note::getVisible)
        );

        Path root = null;
        boolean rootEndsWithData = false;
        if (ragDataRoot != null && !ragDataRoot.isBlank()) {
            root = Paths.get(ragDataRoot).toAbsolutePath().normalize();
            Path leaf = root.getFileName();
            if (leaf != null) {
                rootEndsWithData = "data".equalsIgnoreCase(leaf.toString());
            }
        }

        List<String> paths = new ArrayList<>();
        for (Note n : notes) {
            if (n == null) continue;
            if (Boolean.TRUE.equals(n.getIsFolder())) continue;
            if (!Boolean.TRUE.equals(n.getVisible())) continue;
            if (n.getCurrentPath() == null || n.getCurrentPath().isBlank()) continue;
            String cp = n.getCurrentPath().replace("\\", "/").trim();
            if (cp.startsWith("./")) {
                cp = cp.substring(2);
            }
            Path p = Paths.get(cp).normalize();
            if (!p.isAbsolute()) {
                if (root != null) {
                    String rel = cp;
                    if (rootEndsWithData) {
                        if (rel.startsWith("data/")) {
                            rel = rel.substring("data/".length());
                        } else if ("data".equals(rel)) {
                            rel = "";
                        }
                    }
                    p = root.resolve(rel).normalize();
                } else {
                    p = p.toAbsolutePath().normalize();
                }
            }
            paths.add(p.toString());
        }
        String rootStr = root != null ? root.toString() : ragDataRoot;
        return new ResolvedPaths(paths, notes.size(), rootStr);
    }

    public Result runChunking(String outputPath, int maxChars, int overlap, List<String> exts) {
        ResolvedPaths rp = collectPaths();
        return runChunking(rp, outputPath, maxChars, overlap, exts);
    }

    public Result runChunking(ResolvedPaths rp, String outputPath, int maxChars, int overlap, List<String> exts) {
        List<String> paths = rp.paths();
        log.info("RAG chunk: totalNotes={}, eligiblePaths={}", rp.totalNotes(), paths.size());
        for (int i = 0; i < Math.min(10, paths.size()); i++) {
            log.info("RAG path sample [{}]: {}", i, paths.get(i));
        }

        if (paths.isEmpty()) {
            return new Result(0, 0, outputPath, "no visible notes");
        }

        if (ragPythonUrl == null || ragPythonUrl.isBlank()) {
            throw new IllegalStateException("rag.python.url 未配置，无法调用 Python HTTP 服务");
        }

        var rest = new org.springframework.web.client.RestTemplate();
        var req = new java.util.HashMap<String, Object>();
        req.put("paths", paths);
        if (outputPath != null && !outputPath.isBlank()) {
            req.put("output", outputPath);
        }
        req.put("root", rp.root());
        req.put("max_chars", maxChars);
        req.put("overlap", overlap);
        req.put("exts", exts == null ? java.util.List.of(".md") : exts);

        var resp = rest.postForEntity(ragPythonUrl + "/chunk", req, java.util.Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Python /chunk 调用失败: " + resp.getStatusCode());
        }

        Object fileCountObj = resp.getBody().get("fileCount");
        Object chunkCountObj = resp.getBody().get("chunkCount");
        Object outputObj = resp.getBody().get("output");

        int fileCount = fileCountObj instanceof Number ? ((Number) fileCountObj).intValue() : paths.size();
        int chunkCount = chunkCountObj instanceof Number ? ((Number) chunkCountObj).intValue() : 0;
        String out = outputObj != null ? outputObj.toString() : outputPath;

        return new Result(fileCount, chunkCount, out, String.valueOf(resp.getBody()));
    }

    public PipelineResult runPipeline(ResolvedPaths rp,
                                      String outputPath,
                                      String inputPath,
                                      int maxChars,
                                      int overlap,
                                      List<String> exts,
                                      String collection,
                                      String provider,
                                      String model,
                                      Integer dim,
                                      int batchSize,
                                      boolean normalize,
                                      boolean storeText,
                                      boolean recreate,
                                      Integer logEvery,
                                      String qdrantUrl,
                                      boolean diffFirst,
                                      boolean pruneDeleted,
                                      Integer pruneBatchSize,
                                      boolean pruneAllowEmpty) {
        if (ragPythonUrl == null || ragPythonUrl.isBlank()) {
            throw new IllegalStateException("rag.python.url 未配置，无法调用 Python HTTP 服务");
        }
        var rest = new org.springframework.web.client.RestTemplate();
        var req = new java.util.HashMap<String, Object>();
        req.put("paths", rp.paths());
        if (outputPath != null && !outputPath.isBlank()) {
            req.put("output", outputPath);
        }
        req.put("root", rp.root());
        req.put("max_chars", maxChars);
        req.put("overlap", overlap);
        req.put("exts", exts == null ? java.util.List.of(".md") : exts);
        if (inputPath != null && !inputPath.isBlank()) {
            req.put("input", inputPath);
        }
        req.put("collection", collection);
        if (provider != null && !provider.isBlank()) req.put("provider", provider);
        if (model != null && !model.isBlank()) req.put("model", model);
        if (dim != null) req.put("dim", dim);
        req.put("batch_size", batchSize);
        req.put("normalize", normalize);
        req.put("store_text", storeText);
        req.put("recreate", recreate);
        if (logEvery != null && logEvery > 0) req.put("log_every", logEvery);
        if (qdrantUrl != null && !qdrantUrl.isBlank()) req.put("qdrant_url", qdrantUrl);
        req.put("diff_first", diffFirst);
        req.put("prune_deleted", pruneDeleted);
        if (pruneBatchSize != null && pruneBatchSize > 0) req.put("prune_batch_size", pruneBatchSize);
        req.put("prune_allow_empty", pruneAllowEmpty);

        var resp = rest.postForEntity(ragPythonUrl + "/run", req, java.util.Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Python /run 调用失败: " + resp.getStatusCode());
        }

        var body = resp.getBody();
        RagChunkService.Result chunkRes = new RagChunkService.Result(0, 0, outputPath, String.valueOf(body));
        Object chunkObj = body.get("chunk");
        if (chunkObj instanceof java.util.Map<?, ?> cm) {
            Object fileCountObj = cm.get("fileCount");
            Object chunkCountObj = cm.get("chunkCount");
            Object outputObj = cm.get("output");
            int fileCount = fileCountObj instanceof Number ? ((Number) fileCountObj).intValue() : 0;
            int chunkCount = chunkCountObj instanceof Number ? ((Number) chunkCountObj).intValue() : 0;
            String out = outputObj != null ? outputObj.toString() : outputPath;
            chunkRes = new RagChunkService.Result(fileCount, chunkCount, out, String.valueOf(body));
        }

        RagIngestService.Result ingestRes = new RagIngestService.Result(0, collection, qdrantUrl, String.valueOf(body));
        Object ingestObj = body.get("ingest");
        if (ingestObj instanceof java.util.Map<?, ?> im) {
            Object upsertedObj = im.get("upserted");
            Object collObj = im.get("collection");
            Object qdrantObj = im.get("qdrantUrl");
            int upserted = upsertedObj instanceof Number ? ((Number) upsertedObj).intValue() : 0;
            String coll = collObj != null ? collObj.toString() : collection;
            String qurl = qdrantObj != null ? qdrantObj.toString() : qdrantUrl;
            ingestRes = new RagIngestService.Result(upserted, coll, qurl, String.valueOf(body));
        }

        RagIngestService.DiffResult diffRes = null;
        Object diffObj = body.get("diff");
        if (diffObj instanceof java.util.Map<?, ?> dm) {
            Object currentObj = dm.get("currentDocs");
            Object existingObj = dm.get("existingDocs");
            Object addedObj = dm.get("addedDocs");
            Object changedObj = dm.get("changedDocs");
            Object deletedObj = dm.get("deletedDocs");
            Object missingObj = dm.get("missingFiles");
            int current = currentObj instanceof Number ? ((Number) currentObj).intValue() : 0;
            int existing = existingObj instanceof Number ? ((Number) existingObj).intValue() : 0;
            int added = addedObj instanceof Number ? ((Number) addedObj).intValue() : 0;
            int changed = changedObj instanceof Number ? ((Number) changedObj).intValue() : 0;
            int deleted = deletedObj instanceof Number ? ((Number) deletedObj).intValue() : 0;
            int missing = missingObj instanceof Number ? ((Number) missingObj).intValue() : 0;
            diffRes = new RagIngestService.DiffResult(
                    java.util.List.of(),
                    java.util.List.of(),
                    current,
                    existing,
                    added,
                    changed,
                    deleted,
                    missing,
                    String.valueOf(body)
            );
        }

        RagIngestService.PruneResult pruneRes = null;
        Object pruneObj = body.get("prune");
        if (pruneObj instanceof java.util.Map<?, ?> pm) {
            Object deletedObj = pm.get("deletedDocs");
            Object currentObj = pm.get("currentDocs");
            Object existingObj = pm.get("existingDocs");
            int deleted = deletedObj instanceof Number ? ((Number) deletedObj).intValue() : 0;
            int current = currentObj instanceof Number ? ((Number) currentObj).intValue() : 0;
            int existing = existingObj instanceof Number ? ((Number) existingObj).intValue() : 0;
            pruneRes = new RagIngestService.PruneResult(deleted, current, existing, String.valueOf(body));
        }

        return new PipelineResult(chunkRes, ingestRes, diffRes, pruneRes);
    }

    public record Result(int fileCount, int chunkCount, String outputPath, String rawOutput) {}
}
