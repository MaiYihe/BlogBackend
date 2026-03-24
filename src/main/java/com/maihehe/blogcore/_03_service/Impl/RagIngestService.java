package com.maihehe.blogcore._03_service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class RagIngestService {

    @Value("${rag.python:python3}")
    private String pythonCmd;

    @Value("${rag.qdrant.url:http://localhost:6333}")
    private String qdrantUrlDefault;

    @Value("${rag.embedding.provider:ollama}")
    private String defaultProvider;

    @Value("${rag.embedding.model:}")
    private String defaultModel;

    @Value("${ollama.base-url:}")
    private String ollamaBaseUrl;

    @Value("${ollama.embedding-model:}")
    private String ollamaEmbeddingModel;

    @Value("${rag.python.url:}")
    private String ragPythonUrl;

    @Value("${rag.data.root:data}")
    private String ragDataRoot;

    public String resolveQdrantUrl(String qdrantUrl) {
        return (qdrantUrl == null || qdrantUrl.isBlank()) ? qdrantUrlDefault : qdrantUrl;
    }

    public Result runIngest(String inputPath,
                            String collection,
                            String provider,
                            String model,
                            Integer dim,
                            int batchSize,
                            boolean normalize,
                            boolean storeText,
                            boolean recreate,
                            Integer logEvery,
                            String qdrantUrl) {

        String prov = (provider == null || provider.isBlank()) ? defaultProvider : provider.trim();
        if ("dummy".equalsIgnoreCase(prov) && dim == null) {
            throw new IllegalArgumentException("dummy provider 需要 dim 参数");
        }
        String resolvedModel = (model == null || model.isBlank()) ? defaultModel : model;
        if (("sentence-transformers".equalsIgnoreCase(prov) || "sbert".equalsIgnoreCase(prov))
                && (resolvedModel == null || resolvedModel.isBlank())) {
            throw new IllegalArgumentException("sentence-transformers provider 需要 model 参数");
        }

        String url = resolveQdrantUrl(qdrantUrl);

        if (ragPythonUrl == null || ragPythonUrl.isBlank()) {
            throw new IllegalStateException("rag.python.url 未配置，无法调用 Python HTTP 服务");
        }

        var rest = new org.springframework.web.client.RestTemplate();
        var req = new java.util.HashMap<String, Object>();
        req.put("input", inputPath);
        req.put("collection", collection);
        req.put("provider", prov);
        if (resolvedModel != null && !resolvedModel.isBlank()) req.put("model", resolvedModel);
        if (dim != null) req.put("dim", dim);
        req.put("batch_size", batchSize);
        req.put("normalize", normalize);
        req.put("store_text", storeText);
        req.put("recreate", recreate);
        if (logEvery != null && logEvery > 0) req.put("log_every", logEvery);
        req.put("qdrant_url", url);

        var resp = rest.postForEntity(ragPythonUrl + "/ingest", req, java.util.Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Python /ingest 调用失败: " + resp.getStatusCode());
        }

        Object upsertedObj = resp.getBody().get("upserted");
        int upserted = upsertedObj instanceof Number ? ((Number) upsertedObj).intValue() : 0;
        String raw = String.valueOf(resp.getBody());
        return new Result(upserted, collection, url, raw);
    }

    public record Result(int upserted, String collection, String qdrantUrl, String rawOutput) {}

    public PruneResult pruneDeleted(List<String> paths,
                                    String root,
                                    String collection,
                                    String qdrantUrl,
                                    Integer batchSize,
                                    Boolean allowEmpty) {
        if (paths == null || paths.isEmpty()) {
            return new PruneResult(0, 0, 0, "no paths");
        }
        String url = resolveQdrantUrl(qdrantUrl);
        String resolvedRoot = (root == null || root.isBlank()) ? ragDataRoot : root;

        if (ragPythonUrl == null || ragPythonUrl.isBlank()) {
            throw new IllegalStateException("rag.python.url 未配置，无法调用 Python HTTP 服务");
        }

        var rest = new org.springframework.web.client.RestTemplate();
        var req = new java.util.HashMap<String, Object>();
        req.put("paths", paths);
        req.put("root", resolvedRoot);
        req.put("collection", collection);
        req.put("qdrant_url", url);
        if (batchSize != null && batchSize > 0) req.put("batch_size", batchSize);
        if (allowEmpty != null) req.put("allow_empty", allowEmpty);

        var resp = rest.postForEntity(ragPythonUrl + "/prune", req, java.util.Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Python /prune 调用失败: " + resp.getStatusCode());
        }

        Object deletedObj = resp.getBody().get("deletedDocs");
        Object currentObj = resp.getBody().get("currentDocs");
        Object existingObj = resp.getBody().get("existingDocs");
        int deleted = deletedObj instanceof Number ? ((Number) deletedObj).intValue() : 0;
        int current = currentObj instanceof Number ? ((Number) currentObj).intValue() : 0;
        int existing = existingObj instanceof Number ? ((Number) existingObj).intValue() : 0;
        String raw = String.valueOf(resp.getBody());
        return new PruneResult(deleted, current, existing, raw);
    }

    public record PruneResult(int deletedDocs, int currentDocs, int existingDocs, String rawOutput) {}

    public DiffResult diffPaths(List<String> paths,
                                String root,
                                String collection,
                                String qdrantUrl) {
        if (paths == null || paths.isEmpty()) {
            return new DiffResult(List.of(), List.of(), 0, 0, 0, 0, 0, 0, "no paths");
        }
        String url = resolveQdrantUrl(qdrantUrl);
        String resolvedRoot = (root == null || root.isBlank()) ? ragDataRoot : root;

        if (ragPythonUrl == null || ragPythonUrl.isBlank()) {
            throw new IllegalStateException("rag.python.url 未配置，无法调用 Python HTTP 服务");
        }

        var rest = new org.springframework.web.client.RestTemplate();
        var req = new java.util.HashMap<String, Object>();
        req.put("paths", paths);
        req.put("root", resolvedRoot);
        req.put("collection", collection);
        req.put("qdrant_url", url);

        var resp = rest.postForEntity(ragPythonUrl + "/diff", req, java.util.Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Python /diff 调用失败: " + resp.getStatusCode());
        }

        Object newPathsObj = resp.getBody().get("newPaths");
        List<String> newPaths = new java.util.ArrayList<>();
        if (newPathsObj instanceof java.util.List<?> list) {
            for (Object o : list) {
                if (o != null) newPaths.add(o.toString());
            }
        }
        Object changedPathsObj = resp.getBody().get("changedPaths");
        List<String> changedPaths = new java.util.ArrayList<>();
        if (changedPathsObj instanceof java.util.List<?> list) {
            for (Object o : list) {
                if (o != null) changedPaths.add(o.toString());
            }
        }
        Object currentObj = resp.getBody().get("currentDocs");
        Object existingObj = resp.getBody().get("existingDocs");
        Object addedObj = resp.getBody().get("addedDocs");
        Object changedObj = resp.getBody().get("changedDocs");
        Object deletedObj = resp.getBody().get("deletedDocs");
        Object missingObj = resp.getBody().get("missingFiles");
        int current = currentObj instanceof Number ? ((Number) currentObj).intValue() : 0;
        int existing = existingObj instanceof Number ? ((Number) existingObj).intValue() : 0;
        int added = addedObj instanceof Number ? ((Number) addedObj).intValue() : newPaths.size();
        int changed = changedObj instanceof Number ? ((Number) changedObj).intValue() : changedPaths.size();
        int deleted = deletedObj instanceof Number ? ((Number) deletedObj).intValue() : 0;
        int missing = missingObj instanceof Number ? ((Number) missingObj).intValue() : 0;
        String raw = String.valueOf(resp.getBody());
        return new DiffResult(newPaths, changedPaths, current, existing, added, changed, deleted, missing, raw);
    }

    public record DiffResult(List<String> newPaths,
                             List<String> changedPaths,
                             int currentDocs,
                             int existingDocs,
                             int addedDocs,
                             int changedDocs,
                             int deletedDocs,
                             int missingFiles,
                             String rawOutput) {}

    public DeleteResult deleteDocs(List<String> paths,
                                   String root,
                                   String collection,
                                   String qdrantUrl,
                                   Integer batchSize) {
        if (paths == null || paths.isEmpty()) {
            return new DeleteResult(0, 0, "no paths");
        }
        String url = resolveQdrantUrl(qdrantUrl);
        String resolvedRoot = (root == null || root.isBlank()) ? ragDataRoot : root;

        if (ragPythonUrl == null || ragPythonUrl.isBlank()) {
            throw new IllegalStateException("rag.python.url 未配置，无法调用 Python HTTP 服务");
        }

        var rest = new org.springframework.web.client.RestTemplate();
        var req = new java.util.HashMap<String, Object>();
        req.put("paths", paths);
        req.put("root", resolvedRoot);
        req.put("collection", collection);
        req.put("qdrant_url", url);
        if (batchSize != null && batchSize > 0) req.put("batch_size", batchSize);

        var resp = rest.postForEntity(ragPythonUrl + "/delete_docs", req, java.util.Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Python /delete_docs 调用失败: " + resp.getStatusCode());
        }

        Object deletedObj = resp.getBody().get("deletedDocs");
        Object targetObj = resp.getBody().get("targetDocs");
        int deleted = deletedObj instanceof Number ? ((Number) deletedObj).intValue() : 0;
        int target = targetObj instanceof Number ? ((Number) targetObj).intValue() : paths.size();
        String raw = String.valueOf(resp.getBody());
        return new DeleteResult(deleted, target, raw);
    }

    public record DeleteResult(int deletedDocs, int targetDocs, String rawOutput) {}
}
