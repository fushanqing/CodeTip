package top.binggo.codetipcontrolcenter.service;

import codetip.commons.bean.Task;
import codetip.commons.bean.TaskType;
import codetip.commons.crawl.RedisTaskAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.binggo.codetip.filesystem.core.MavenRedisFileSystemAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author binggo
 */
@Component
@Slf4j
public class NewTaskProcessors implements BiFunction<String, Set<String>, Task> {

    private final RedisTaskAccessor redisTaskAccessor;
    private Map<String, Function<Set<String>, Task>> map = new HashMap<>(32);

    @Autowired
    public NewTaskProcessors(RedisTaskAccessor redisTaskAccessor) {
        this.redisTaskAccessor = redisTaskAccessor;
        map.put(TaskType.UPDATE_DIRECTORY.toString(), new UpdateDirectoryProcessor());
        map.put(TaskType.DOWNLOAD_AND_SOURCE_ANALYSIS.toString(), new AnalysisProjectProcessor());
    }

    @Override
    public Task apply(String s, Set<String> strings) {
        Function<Set<String>, Task> function = map.get(s);
        if (function == null) {
            return null;
        }
        return function.apply(strings);
    }


    private class UpdateDirectoryProcessor implements Function<Set<String>, Task> {

        @Override
        public Task apply(Set<String> targetFolder) {
            targetFolder = targetFolder.stream().map(MavenRedisFileSystemAccessor::absoluteFolderName2Url).collect(Collectors.toSet());
            log.info("new CrawlTask for set={}", targetFolder);
            return redisTaskAccessor.newCrawlTask(targetFolder, "admin");
        }
    }

    private class AnalysisProjectProcessor implements Function<Set<String>, Task> {

        @Override
        public Task apply(Set<String> targetFolder) {

            Set<String> collect = targetFolder.stream().filter(s -> s.endsWith(".xml")).collect(Collectors.toSet());
            if (collect.isEmpty()) {
                return null;
            }
            log.info("new AnalysisTask for set={}", collect);
            return redisTaskAccessor.newAnalysisTask(collect, "admin");
        }
    }

}
