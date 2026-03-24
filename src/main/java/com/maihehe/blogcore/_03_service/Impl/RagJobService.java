package com.maihehe.blogcore._03_service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class RagJobService {
    private final Executor ragExecutor;
    private final RagChunkService ragChunkService;
    private final RagIngestService ragIngestService;

    private final Map<String, RagJob> jobs = new ConcurrentHashMap<>();

    public RagJobService(@Qualifier("ragTaskExecutor") Executor ragExecutor,
                         RagChunkService ragChunkService,
                         RagIngestService ragIngestService) {
        this.ragExecutor = ragExecutor;
        this.ragChunkService = ragChunkService;
        this.ragIngestService = ragIngestService;
    }

    public String startAsync(RagPipelineRequest req) {
        String jobId = UUID.randomUUID().toString();
        RagJob job = new RagJob(jobId);
        job.setStatus(Status.QUEUED);
        jobs.put(jobId, job);

        ragExecutor.execute(() -> runJob(jobId, req));
        return jobId;
    }

    public RagJob getJob(String jobId) {
        return jobs.get(jobId);
    }

    private void runJob(String jobId, RagPipelineRequest req) {
        RagJob job = jobs.get(jobId);
        if (job == null) return;
        job.setStatus(Status.RUNNING);
        job.setStartedAt(Instant.now());
        try {
            RagChunkService.ResolvedPaths rp = ragChunkService.collectPaths();
            RagChunkService.PipelineResult res = ragChunkService.runPipeline(
                    rp,
                    req.outputPath(),
                    req.inputPath(),
                    req.maxChars(),
                    req.overlap(),
                    req.exts(),
                    req.collection(),
                    req.provider(),
                    req.model(),
                    req.dim(),
                    req.batchSize(),
                    req.normalize(),
                    req.storeText(),
                    req.recreate(),
                    req.logEvery(),
                    req.qdrantUrl(),
                    req.diffFirst(),
                    req.pruneDeleted(),
                    req.pruneBatchSize(),
                    req.pruneAllowEmpty()
            );
            job.setChunkResult(res.chunkResult());
            job.setIngestResult(res.ingestResult());
            job.setDiffResult(res.diffResult());
            job.setPruneResult(res.pruneResult());
            job.setStatus(Status.SUCCESS);
        } catch (Exception e) {
            log.error("RAG pipeline failed, jobId={}", jobId, e);
            job.setStatus(Status.FAILED);
            job.setError(e.getMessage());
        } finally {
            job.setFinishedAt(Instant.now());
        }
    }

    public enum Status {
        QUEUED, RUNNING, SUCCESS, FAILED
    }

    public static class RagJob {
        private final String id;
        private volatile Status status;
        private volatile Instant startedAt;
        private volatile Instant finishedAt;
        private volatile String error;
        private volatile RagChunkService.Result chunkResult;
        private volatile RagIngestService.Result ingestResult;
        private volatile RagIngestService.PruneResult pruneResult;
        private volatile RagIngestService.DiffResult diffResult;

        public RagJob(String id) {
            this.id = id;
        }

        public String getId() { return id; }
        public Status getStatus() { return status; }
        public Instant getStartedAt() { return startedAt; }
        public Instant getFinishedAt() { return finishedAt; }
        public String getError() { return error; }
        public RagChunkService.Result getChunkResult() { return chunkResult; }
        public RagIngestService.Result getIngestResult() { return ingestResult; }
        public RagIngestService.PruneResult getPruneResult() { return pruneResult; }
        public RagIngestService.DiffResult getDiffResult() { return diffResult; }

        void setStatus(Status status) { this.status = status; }
        void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
        void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
        void setError(String error) { this.error = error; }
        void setChunkResult(RagChunkService.Result chunkResult) { this.chunkResult = chunkResult; }
        void setIngestResult(RagIngestService.Result ingestResult) { this.ingestResult = ingestResult; }
        void setPruneResult(RagIngestService.PruneResult pruneResult) { this.pruneResult = pruneResult; }
        void setDiffResult(RagIngestService.DiffResult diffResult) { this.diffResult = diffResult; }
    }

    public record RagPipelineRequest(
            String outputPath,
            int maxChars,
            int overlap,
            List<String> exts,
            String inputPath,
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
            boolean pruneAllowEmpty
    ) {}
}
