package top.binggo.codetipcontrolcenter.service;

import codetip.commons.bean.CrawlerTaskStatus;
import codetip.commons.bean.Task;
import codetip.commons.crawl.RedisTaskAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author binggo
 */
@Service
public class CrawlerTaskService {


    @Autowired
    private RedisTaskAccessor redisTaskAccessor;

    public List<Task> listTask(CrawlerTaskStatus crawlerTaskStatus) {
        return redisTaskAccessor.getAllTaskInPool(crawlerTaskStatus);
    }

    public String startATask(String taskId) {
        return redisTaskAccessor.startATask(taskId);
    }

    public void stopTask(String taskId) {

        redisTaskAccessor.stopTask(taskId);
    }
}
