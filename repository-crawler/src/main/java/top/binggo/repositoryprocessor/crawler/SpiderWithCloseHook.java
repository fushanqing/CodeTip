package top.binggo.repositoryprocessor.crawler;

import codetip.commons.bean.CrawlerTaskStatus;
import codetip.commons.bean.Task;
import codetip.commons.crawl.RedisTaskAccessor;
import lombok.extern.slf4j.Slf4j;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.ArrayList;

/**
 * 相比原生的Spider对象多了一个任务执行结束
 * 的钩子函数,用于将任务的状态在任务队列中
 * 进行转移
 *
 * @author binggo
 */
@Slf4j
public class SpiderWithCloseHook extends Spider {

    private RedisTaskAccessor redisTaskAccessor;
    private CrawlerApplication crawlerApplication;

    /**
     * create a spider with pageProcessor.
     *
     * @param pageProcessor pageProcessor
     */
    public SpiderWithCloseHook(PageProcessor pageProcessor, RedisTaskAccessor redisTaskAccessor, CrawlerApplication crawlerApplication) {
        super(pageProcessor);
        this.redisTaskAccessor = redisTaskAccessor;
        this.crawlerApplication = crawlerApplication;
    }

    public static SpiderWithCloseHook create(PageProcessor pageProcessor, RedisTaskAccessor redisTaskAccessor, CrawlerApplication crawlerApplication) {
        return new SpiderWithCloseHook(pageProcessor, redisTaskAccessor, crawlerApplication);
    }

    @Override
    public void run() {
        super.run();
        String s = closeHook();
        log.info("{} finish {}", getUUID(), s != null ? "success" : "fail");
    }

    protected String closeHook() {
        String taskId = getUUID();
        crawlerApplication.getRunningSpider().remove(taskId);
        String s = redisTaskAccessor.finishATask(taskId);
        redisTaskAccessor.setTaskResultMsgInStatus(CrawlerTaskStatus.FINISHED, taskId, "process " + getPageCount() + " pages");
        Task aTaskInStatus = redisTaskAccessor.getATaskInStatus(CrawlerTaskStatus.UN_START, null);
        if (aTaskInStatus != null) {
            crawlerApplication.apply(aTaskInStatus.getTaskId(), new ArrayList<>(aTaskInStatus.getContent()));
        }
        return s;
    }
}
