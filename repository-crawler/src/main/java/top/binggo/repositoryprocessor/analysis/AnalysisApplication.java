package top.binggo.repositoryprocessor.analysis;

import codetip.commons.bean.MavenMetadataPublicBean;
import codetip.commons.crawl.RedisTaskAccessor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.binggo.codetip.filesystem.core.MavenMetadataReader;
import top.binggo.codetip.filesystem.core.MavenRedisFileSystemAccessor;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.processor.ProcessAndIndexProcessor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.BiFunction;

/**
 * @author binggo
 */
@Slf4j
@Component
public class AnalysisApplication implements BiFunction<String, List<String>, String> {
    public static final String ANALYSIS_APPLICATION_NAME = "Analysis/" + UUID.randomUUID().toString();
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 5;
    private final
    ProcessAndIndexProcessor processAndIndexProcessor;
    private final
    MavenRedisFileSystemAccessor mavenRedisFileSystemAccessor;
    private final
    RedisTaskAccessor redisTaskAccessor;
    private ExecutorService executorService = new ThreadPoolExecutor(CORE_POOL_SIZE
            , CORE_POOL_SIZE * 2
            , 1000, TimeUnit.MILLISECONDS
            , new LinkedBlockingDeque<>(1 << 8), new ThreadFactoryBuilder().setNameFormat("Spider-%d")
            .build(), (r, executor) -> log.warn("some task has bean abandoned")
    );

    public final Map<String, Analyst> runningAnalyst = new ConcurrentHashMap<>();


    @Autowired
    public AnalysisApplication(ProcessAndIndexProcessor processAndIndexProcessor, MavenRedisFileSystemAccessor mavenRedisFileSystemAccessor, RedisTaskAccessor redisTaskAccessor) {
        this.processAndIndexProcessor = processAndIndexProcessor;
        this.mavenRedisFileSystemAccessor = mavenRedisFileSystemAccessor;
        this.redisTaskAccessor = redisTaskAccessor;
    }

    @Override
    public String apply(String taskId, List<String> strings) {
        for (String s : strings) {
            //获得的对应的maven坐标

            String key = mavenRedisFileSystemAccessor.getRedisContentAccessor().valueGet(s.substring("#root/".length()));
            MavenMetadataPublicBean mavenMetadataPublicBean = null;
            try {
                mavenMetadataPublicBean = MavenMetadataReader.getMavenMetadataPublicBean(key);

            } catch (IOException e) {
                log.error("process url={} meet {}", s, e);
                continue;
            }
            String subTaskId = taskId + s;
            Analyst task = new Analyst(subTaskId, this, mavenMetadataPublicBean.getGroupId(), mavenMetadataPublicBean.getArtifactId(), mavenMetadataPublicBean.getRecommendVersion(), processAndIndexProcessor, redisTaskAccessor, taskId, mavenRedisFileSystemAccessor);
            runningAnalyst.put(subTaskId, task);
            executorService.submit(task);
        }
        return taskId;
    }
}
