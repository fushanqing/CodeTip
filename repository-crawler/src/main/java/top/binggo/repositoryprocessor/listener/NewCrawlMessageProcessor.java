package top.binggo.repositoryprocessor.listener;


import codetip.commons.bean.Task;
import codetip.commons.bean.TaskType;
import codetip.commons.crawl.RedisTaskAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import top.binggo.repositoryprocessor.crawler.CrawlerApplication;

/**
 * 用来redis中发布的新建任务的事件(即UN_START事件)
 *
 * @author binggo
 */
@Slf4j
public class NewCrawlMessageProcessor extends AbstractMessageProcessor {
    private static final int MAX_PROCESS_TASK_NUM = 5;

    public NewCrawlMessageProcessor(CrawlerApplication crawlerApplication, RedisTaskAccessor redisTaskAccessor, String eventPattern) {
        super(crawlerApplication, redisTaskAccessor, eventPattern);
    }

    @Override
    public void accept(Message message) {
        while (true) {
            Task task = takeTaskByType(TaskType.UPDATE_DIRECTORY);
            boolean continueFlag = false;
            if (task != null) {
                int size = ((CrawlerApplication) application).getRunningSpider().size();
                if (size < MAX_PROCESS_TASK_NUM) {
                    process0(task, message);
                    continueFlag = true;
                }
            }
            if (!continueFlag) {
                break;
            }
        }
    }


}
