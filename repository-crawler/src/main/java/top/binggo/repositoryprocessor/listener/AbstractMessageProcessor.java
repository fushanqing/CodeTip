package top.binggo.repositoryprocessor.listener;

import codetip.commons.bean.CrawlerTaskStatus;
import codetip.commons.bean.Task;
import codetip.commons.bean.TaskType;
import codetip.commons.crawl.RedisTaskAccessor;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * @author binggo
 */
@Slf4j
public abstract class AbstractMessageProcessor implements Consumer<Message> {
    public final String eventPattern;
    protected BiFunction<String, List<String>, String> application;
    protected RedisTaskAccessor redisTaskAccessor;

    public AbstractMessageProcessor(BiFunction<String, List<String>, String> application, RedisTaskAccessor redisTaskAccessor, String eventPattern) {
        this.application = application;
        this.redisTaskAccessor = redisTaskAccessor;
        this.eventPattern = eventPattern;
    }


    Task takeTaskByType(TaskType taskType) {
        List<Task> allTaskInPool = redisTaskAccessor.getAllTaskInPool(CrawlerTaskStatus.UN_START);
        Optional<Task> any = allTaskInPool.stream().filter(task -> task.getTaskType() == taskType).findAny();
        return any.map(task -> redisTaskAccessor.transformTaskFromStatusA2StatusB(task.getTaskId(), CrawlerTaskStatus.UN_START, CrawlerTaskStatus.PROCESSING)).orElse(null);

    }

    protected void process0(Task task, Message message) {
        if (task != null) {
            log.info("get the message:{} ,begin to process the task:{}", message, task.getTaskId());
            application.apply(task.getTaskId(), Lists.newArrayList(task.getContent()));
        } else {
            log.info("can't process Message:{},because can't transform any task from status UN_START to status.PROCESSING", message);
        }
    }
}
