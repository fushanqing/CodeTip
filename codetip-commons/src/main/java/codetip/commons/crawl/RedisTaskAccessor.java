package codetip.commons.crawl;

import codetip.commons.bean.CrawlerTaskStatus;
import codetip.commons.bean.Task;
import codetip.commons.bean.TaskType;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用于访问redis中的爬虫任务队列
 * 不同状态的任务存储在redis的不
 * 同的队列当中
 *
 * @author binggo
 */
@Slf4j
public class RedisTaskAccessor {
    public static final String ACCESS_KEY = "codeTip-task#";

    private RedisTemplate<String, Task> jsonRedisTemplate;
    private StringRedisTemplate stringRedisTemplate;

    public RedisTaskAccessor(RedisTemplate<String, Task> jsonRedisTemplate, StringRedisTemplate stringRedisTemplate) {
        this.jsonRedisTemplate = jsonRedisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * @param url 要爬取的目标路径
     * @return Pair(taskId, url really will Crawl
     */
    public Task newCrawlTask(@NonNull Set<String> url, String operation) {
        Set<String> urlToCrawl = filterUrl(url);
        if (!CollectionUtils.isEmpty(urlToCrawl)) {
            Task task = getANewTask(urlToCrawl, operation, TaskType.UPDATE_DIRECTORY);
            addTaskToPool(CrawlerTaskStatus.UN_START, task);
            publishTaskEventForPool(CrawlerTaskStatus.UN_START, task.getTaskId());
            log.info("new Crawl Task={}", task.getTaskId());
            return task;
        }
        return null;
    }

    private static String nowTimeString() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public Task setTaskResultMsgInStatus(CrawlerTaskStatus crawlerTaskStatus, String taskId, String resultMsg) {
        List<Task> allTaskInPool = getAllTaskInPool(crawlerTaskStatus);
        if (!CollectionUtils.isEmpty(allTaskInPool)) {
            Optional<Task> any = allTaskInPool.stream().filter(task -> taskId.equals(task.getTaskId())).findAny();
            if (any.isPresent()) {
                Task task = any.get();
                task.setResultMsg(resultMsg);
                refreshStatusInTaskPool(crawlerTaskStatus, task);
                return task;
            }
        }
        return null;
    }

    public String startATask(String taskId) {
        Preconditions.checkNotNull(taskId);
        List<Task> allTaskInPool = getAllTaskInPool(CrawlerTaskStatus.UN_START);
        if (!CollectionUtils.isEmpty(allTaskInPool)) {
            Optional<Task> any = allTaskInPool.stream().filter(task -> taskId.equals(task.getTaskId())).findAny();
            if (any.isPresent()) {
                publishTaskEventForPool(CrawlerTaskStatus.UN_START, taskId);
                return taskId;
            }
        }
        return null;
    }

    public Task newAnalysisTask(@NonNull Set<String> url, String operation) {
        if (!CollectionUtils.isEmpty(url)) {
            Task task = getANewTask(url, operation, TaskType.DOWNLOAD_AND_SOURCE_ANALYSIS);
            addTaskToPool(CrawlerTaskStatus.UN_START, task);
            publishTaskEventForPool(CrawlerTaskStatus.UN_START, task.getTaskId());
            log.info("new Analysis Task={}", task.getTaskId());
            return task;
        }
        return null;
    }

    private Task refreshStatusInTaskPool(CrawlerTaskStatus crawlerTaskStatus, Task task) {
        Preconditions.checkNotNull(crawlerTaskStatus);
        if (task == null || task.getTaskId() == null) {
            return null;
        }
        task.setLastModifiedTime(nowTimeString());
        Task ret = jsonRedisTemplate.<String, Task>opsForHash().get(crawlerTaskStatus.getRedisKey(), task.getTaskId());
        if (ret != null) {
            jsonRedisTemplate.<String, Task>opsForHash().put(crawlerTaskStatus.getRedisKey(), task.getTaskId(), task);
        }
        return ret;
    }

    public void stopTask(@NonNull String taskId) {
        Task aTaskInStatus = getATaskInStatus(CrawlerTaskStatus.UN_START, taskId);
        if (aTaskInStatus != null) {
            transformTaskFromStatusA2StatusB(taskId, CrawlerTaskStatus.UN_START, CrawlerTaskStatus.FINISHED);
            log.info("stop the UN_START task={}", taskId);
        } else if (getATaskInStatus(CrawlerTaskStatus.PROCESSING, taskId) != null) {
            publishTaskEventForPool(CrawlerTaskStatus.PROCESSING, "" + taskId);
            log.info("public {} event for {}", CrawlerTaskStatus.PROCESSING.toString(), taskId);
        } else {
            log.info("taskId={} is not in UN_START Queue or PROCESSING queue", taskId);
        }
    }



    /**
     * @param taskId 要结束的任务的任务ID
     * @return 是否结束成功, 如果返回 {@code null}
     * 很可能是 CrawlerTaskStatus.PROCESSING 的
     * 任务队列中没有该ID的任务
     */
    public String finishATask(@NonNull String taskId) {
        String s = transformTaskFromStatusA2StatusB(taskId, CrawlerTaskStatus.PROCESSING, CrawlerTaskStatus.FINISHED) != null ? taskId : null;
        log.info("finish task={} {}", taskId, taskId.equals(s) ? "success" : "fail");
        return s;
    }


    /**
     * 返回对应状态的所有任务
     *
     * @param crawlerTaskStatus 任务的状态
     * @return 任务池中获得的所有任务
     */
    public List<Task> getAllTaskInPool(@Nullable CrawlerTaskStatus crawlerTaskStatus) {
        List<Task> tasks = new ArrayList<>(32);
        List<CrawlerTaskStatus> targetStatus;
        if (crawlerTaskStatus == null) {
            targetStatus = Arrays.asList(CrawlerTaskStatus.values());
        } else {
            targetStatus = Lists.newArrayList(crawlerTaskStatus);
        }
        for (CrawlerTaskStatus status : targetStatus) {
            tasks.addAll(getTaskInPool(status));
        }
        return tasks;
    }

    /**
     * @param crawlerTaskStatus 任务的状态，为null表示不指定任务的状态
     * @param taskId            任务的id，为null表示不指定任务的id
     * @return 满足条件的任务或者null
     */
    public Task getATaskInStatus(@Nullable CrawlerTaskStatus crawlerTaskStatus, String taskId) {
        List<Task> allTaskInPool = getAllTaskInPool(crawlerTaskStatus);
        if (!CollectionUtils.isEmpty(allTaskInPool)) {
            if (taskId == null) {
                return allTaskInPool.get(0);
            } else {
                Optional<Task> any = allTaskInPool.stream().filter(task -> taskId.equals(task.getTaskId())).findAny();
                if (any.isPresent()) {
                    return any.get();
                }
            }
        }
        return null;
    }

    /**
     * 将taskId任务从statusA状态转化成statusB状态
     */
    public Task transformTaskFromStatusA2StatusB(@Nullable String taskId, CrawlerTaskStatus statusA, CrawlerTaskStatus statusB) {
        //获得的任务队列
        List<Task> allTaskInPool = getAllTaskInPool(statusA);
        if (allTaskInPool == null || allTaskInPool.isEmpty()) {
            return null;
        }
        //获得任务队列中的指定任务或是,某一个任务(taskId==null)时
        boolean processFlag = false;
        final Task task;
        if (taskId == null) {
            processFlag = true;
            task = allTaskInPool.get(0);
        } else {
            Optional<Task> any = allTaskInPool.stream().filter(t -> Objects.equals(taskId, t.getTaskId())).findAny();
            if (any.isPresent()) {
                processFlag = true;
                task = any.get();
            } else {
                task = null;
            }
        }
        //转移任务//todo 转移失败重试
        if (processFlag) {
            Boolean execute = jsonRedisTemplate.execute(new SessionCallback<Boolean>() {
                @Override
                @SuppressWarnings("unchecked")
                public Boolean execute(@Nullable RedisOperations operations) {
                    //todo 队列未提交的时候 返回null
                    if (operations != null) {
                        operations.watch(Lists.newArrayList(statusA.getRedisKey(), statusB.getRedisKey()));
                        operations.multi();
                        operations.opsForHash().delete(statusA.getRedisKey(), task.getTaskId());
                        task.setStatus(statusB);
                        task.setLastModifiedTime(nowTimeString());
                        operations.opsForHash().put(statusB.getRedisKey(), task.getTaskId(), task);
                        operations.exec();
                        return true;
                    }
                    return false;
                }
            });
            //转移成功返回,转移成功的taskId,否则返回null
            Task task1 = execute != null && execute ? task : null;
            log.info("transform Task={} From StatusA={} to StatusB={} {}", task.getTaskId(), statusA, statusB, task1 != null ? "success" : "fail");
            return task1;
        }
        return null;
    }

    private Task getANewTask(Set<String> urlToCrawl, String operation, TaskType taskType) {
        String newTaskId = UUID.randomUUID().toString();
        String format = nowTimeString();
        return Task.builder().createTime(format).lastModifiedTime(format).status(CrawlerTaskStatus.UN_START).operator(operation).content(urlToCrawl).taskId(newTaskId).taskType(taskType).resultMsg(CrawlerTaskStatus.UN_START.toString()).build();
    }

    private void addTaskToPool(CrawlerTaskStatus taskStatus, Task task) {
        jsonRedisTemplate.opsForHash().put(taskStatus.getRedisKey(), task.getTaskId(), task);
    }

    private Collection<Task> getTaskInPool(CrawlerTaskStatus crawlerTaskStatus) {
        return jsonRedisTemplate.<String, Task>opsForHash().entries(crawlerTaskStatus.getRedisKey()).values();
    }


    /**
     * 为某个队列发布一个事件
     * <pre>
     *  比如:
     *  发送一个事件,告诉爬取程序<em>可能</em>新的爬取任务了,赶快来拉取新的任务
     * </pre>
     */
    private void publishTaskEventForPool(CrawlerTaskStatus crawlerTaskStatus, String msg) {
        stringRedisTemplate.convertAndSend(crawlerTaskStatus.getRedisKey(), msg != null ? msg : "");
        log.info("publish a task of status:{} with msg:{}", crawlerTaskStatus, msg);
    }

    /**
     * 将url2Filter和目前已经在处理的task(在redis中以一个队列的形式存在)进行比较过滤掉重复的url并进行返回
     *
     * @param url2Filter 等待被过滤的url
     * @return 过滤完成的url
     */
    private Set<String> filterUrl(Set<String> url2Filter) {
        //保证url2Filter中的url不相互为前缀
        Iterator<String> iterator = url2Filter.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            if (hasPrefixIn(next, url2Filter)) {
                iterator.remove();
            }
        }
        Collection<Task> range = getTaskInPool(CrawlerTaskStatus.PROCESSING);
        if (CollectionUtils.isEmpty(range)) {
            return url2Filter;
        }
        List<Set<String>> collect = range.stream().map(Task::getContent).collect(Collectors.toList());
        Set<String> strings = new HashSet<>();
        for (Set<String> stringList : collect) {
            strings.addAll(stringList);
        }
        return url2Filter.stream().filter(s -> canNotReachFrom(s, strings)).collect(Collectors.toSet());
    }

    private boolean hasPrefixIn(String s, Set<String> strings) {
        for (String string : strings) {
            if (!s.equals(string) && s.startsWith(string)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 如果url1是url2的前缀,则表示url2被url1包含.返回false
     *
     * @param url url2
     * @param set url1的集合
     * @return url是否不被set中的任何一个url包含
     */
    private boolean canNotReachFrom(String url, Set<String> set) {
        for (String s : set) {
            if (url.startsWith(s)) {
                return false;
            }
        }
        return true;
    }


}
