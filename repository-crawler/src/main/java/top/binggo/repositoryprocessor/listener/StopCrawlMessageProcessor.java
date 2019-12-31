package top.binggo.repositoryprocessor.listener;

import codetip.commons.bean.CrawlerTaskStatus;
import codetip.commons.bean.Task;
import codetip.commons.crawl.RedisTaskAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import top.binggo.repositoryprocessor.crawler.CrawlerApplication;

/**
 * @author binggo
 */
@Slf4j
public class StopCrawlMessageProcessor extends AbstractMessageProcessor {


    public StopCrawlMessageProcessor(CrawlerApplication crawlerApplication, RedisTaskAccessor redisTaskAccessor, String eventPattern) {
        super(crawlerApplication, redisTaskAccessor, eventPattern);
    }

    @Override
    public void accept(Message message) {
        String taskId = new String(message.getBody());
        if (((CrawlerApplication) application).getRunningSpider().containsKey(taskId)) {
            log.info("taskId:{} are bean processed by CrawlApplication named {}", taskId, CrawlerApplication.CRAWLER_APPLICATION_NAME);
            Task task = redisTaskAccessor.transformTaskFromStatusA2StatusB(taskId, CrawlerTaskStatus.PROCESSING, CrawlerTaskStatus.STOPPED);
            if (task != null) {
                ((CrawlerApplication) application).getRunningSpider().get(task.getTaskId()).stop();
                log.info("taskId:{} has bean stopped by CrawlApplication named {}", task.getTaskId(), CrawlerApplication.CRAWLER_APPLICATION_NAME);
            }
        } else {
            log.info("can not process this Message:{},because taskId:{} is not bean process by this crawlApplication:{}", message, taskId, CrawlerApplication.CRAWLER_APPLICATION_NAME);
        }
    }
}
