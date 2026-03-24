package com.maihehe.blogcore._03_service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class RagChatService {

    @Value("${rag.python.url:}")
    private String ragPythonUrl;

    private final ThreadPoolTaskExecutor ragExecutor;
    private final RestTemplate restTemplate = new RestTemplate();
    private final WebClient webClient;

    public RagChatService(@Qualifier("ragTaskExecutor") ThreadPoolTaskExecutor ragExecutor) {
        this.ragExecutor = ragExecutor;
        this.webClient = WebClient.builder().build();
    }

    public CompletableFuture<Map<String, Object>> chatAsync(Map<String, Object> reqBody,
                                                            String sessionId,
                                                            String userId,
                                                            String clientIp) {
        if (ragPythonUrl == null || ragPythonUrl.isBlank()) {
            throw new IllegalStateException("rag.python.url 未配置，无法调用 Python HTTP 服务");
        }

        String uid = (userId == null || userId.isBlank()) ? "anonymous" : userId;
        String ip = (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp;
        String sid;
        Object reqSid = reqBody == null ? null : reqBody.get("session_id");
        if (sessionId != null && !sessionId.isBlank()) {
            sid = sessionId;
        } else if (reqSid != null && !String.valueOf(reqSid).isBlank()) {
            sid = String.valueOf(reqSid);
        } else {
            sid = uid;
        }
        log.info("RAG chat request: session_id={}, userId={}, ip={}", sid, uid, ip);
        logPoolStats("enqueue", sid, uid, ip);

        var payload = new java.util.HashMap<String, Object>();
        if (reqBody != null) {
            payload.putAll(reqBody);
        }
        payload.put("user_id", uid);
        payload.put("client_ip", ip);
        if (!payload.containsKey("session_id")) {
            payload.put("session_id", sid);
        }

        return CompletableFuture.supplyAsync(() -> {
            logPoolStats("start", sid, uid, ip);
            // 1) blocking query for paths (inside async executor)
            ResponseEntity<Map> queryResp = restTemplate.postForEntity(
                    ragPythonUrl + "/rag/query?paths_only=true",
                    payload,
                    Map.class
            );
            if (!queryResp.getStatusCode().is2xxSuccessful() || queryResp.getBody() == null) {
                throw new IllegalStateException("Python /rag/query 调用失败: " + queryResp.getStatusCode());
            }

            Object pathsObj = queryResp.getBody().get("paths");
            List<?> paths = (pathsObj instanceof List<?> list) ? list : List.of();
            log.info("RAG query paths: session_id={}, userId={}, ip={}, paths={}", sid, uid, ip, paths);

            // 2) submit async chat task
            ResponseEntity<Map> submitResp = restTemplate.postForEntity(
                    ragPythonUrl + "/rag/chat/submit",
                    payload,
                    Map.class
            );
            if (!submitResp.getStatusCode().is2xxSuccessful() || submitResp.getBody() == null) {
                throw new IllegalStateException("Python /rag/chat/submit 调用失败: " + submitResp.getStatusCode());
            }
            Object taskIdObj = submitResp.getBody().get("task_id");
            String taskId = taskIdObj == null ? "" : String.valueOf(taskIdObj);

            var result = new java.util.HashMap<String, Object>();
            result.put("taskId", taskId);
            result.put("paths", paths);
            result.put("streamUrl", "/api/rag/chat/stream/" + taskId);
            logPoolStats("done", sid, uid, ip);
            return result;
        }, ragExecutor);
    }

    public SseEmitter streamTask(String taskId, String userId, String clientIp) {
        if (ragPythonUrl == null || ragPythonUrl.isBlank()) {
            throw new IllegalStateException("rag.python.url 未配置，无法调用 Python HTTP 服务");
        }
        SseEmitter emitter = new SseEmitter(0L);
        String uid = (userId == null || userId.isBlank()) ? "anonymous" : userId;
        String ip = (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp;
        log.info("RAG stream open: taskId={}, userId={}, ip={}", taskId, uid, ip);
        String baseUrl = ragPythonUrl == null ? "" : ragPythonUrl.trim();
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "http://" + baseUrl;
        }
        final URI streamUri;
        try {
            streamUri = URI.create(baseUrl + "/rag/chat/stream/" + taskId);
            if (streamUri.getHost() == null) {
                throw new IllegalArgumentException("Host is not specified");
            }
        } catch (Exception e) {
            log.error("RAG stream url invalid: baseUrl={}, taskId={}", baseUrl, taskId, e);
            throw new IllegalStateException("rag.python.url 不合法，无法创建 stream URL: " + baseUrl, e);
        }
        log.info("RAG stream url: {}", streamUri);
        AtomicBoolean done = new AtomicBoolean(false);
        AtomicBoolean cancelSent = new AtomicBoolean(false);
        Runnable cancelIfNeeded = () -> {
            if (done.get()) {
                return;
            }
            if (cancelSent.compareAndSet(false, true)) {
                ragExecutor.execute(() -> {
                    try {
                        cancelTask(taskId);
                        log.info("RAG stream cancelled: taskId={}, userId={}, ip={}", taskId, uid, ip);
                    } catch (Exception e) {
                        log.warn("RAG stream cancel failed: taskId={}, userId={}, ip={}, err={}",
                                taskId, uid, ip, e.toString());
                    }
                });
            }
        };
        Disposable disposable = webClient
                .get()
                .uri(streamUri)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .subscribe(event -> {
                            String name = event.event() == null ? "message" : event.event();
                            String data = event.data() == null ? "" : event.data();
                            try {
                                if ("done".equals(name)) {
                                    done.set(true);
                                    log.info("RAG stream done: taskId={}, userId={}, ip={}", taskId, uid, ip);
                                }
                                emitter.send(SseEmitter.event().name(name).data(data));
                            } catch (Exception ex) {
                                emitter.completeWithError(ex);
                            }
                        },
                        ex -> {
                            cancelIfNeeded.run();
                            emitter.completeWithError(ex);
                        },
                        emitter::complete);

        emitter.onCompletion(() -> {
            log.info("RAG stream complete: taskId={}, userId={}, ip={}", taskId, uid, ip);
            disposable.dispose();
            cancelIfNeeded.run();
        });
        emitter.onTimeout(() -> {
            log.warn("RAG stream timeout: taskId={}, userId={}, ip={}", taskId, uid, ip);
            disposable.dispose();
            cancelIfNeeded.run();
        });
        emitter.onError(ex -> {
            log.warn("RAG stream error: taskId={}, userId={}, ip={}, err={}", taskId, uid, ip, ex.toString());
            disposable.dispose();
            cancelIfNeeded.run();
        });
        return emitter;
    }

    public Map<String, Object> cancelTask(String taskId) {
        if (ragPythonUrl == null || ragPythonUrl.isBlank()) {
            throw new IllegalStateException("rag.python.url 未配置，无法调用 Python HTTP 服务");
        }
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                ragPythonUrl + "/rag/chat/cancel/" + taskId,
                null,
                Map.class
        );
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Python /rag/chat/cancel 调用失败: " + resp.getStatusCode());
        }
        //noinspection unchecked
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        log.info("RAG cancel executed: taskId={}, response={}", taskId, body);
        return body;
    }

    private void logPoolStats(String stage, String sid, String uid, String ip) {
        try {
            ThreadPoolExecutor tpe = ragExecutor.getThreadPoolExecutor();
            if (tpe == null) {
                return;
            }
            int active = tpe.getActiveCount();
            int poolSize = tpe.getPoolSize();
            int max = tpe.getMaximumPoolSize();
            int queued = tpe.getQueue().size();
            long completed = tpe.getCompletedTaskCount();
            long taskCount = tpe.getTaskCount();
            log.info(
                    "RAG executor {}: active={}, poolSize={}, max={}, queued={}, completed={}, taskCount={}, session_id={}, userId={}, ip={}",
                    stage, active, poolSize, max, queued, completed, taskCount, sid, uid, ip
            );
        } catch (Exception ignored) {
        }
    }
}
