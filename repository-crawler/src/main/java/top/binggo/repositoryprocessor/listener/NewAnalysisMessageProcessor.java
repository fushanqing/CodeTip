package top.binggo.repositoryprocessor.listener;

import codetip.commons.bean.Task;
import codetip.commons.bean.TaskType;
import codetip.commons.crawl.RedisTaskAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import top.binggo.repositoryprocessor.analysis.AnalysisApplication;

/**
 * @author binggo
 */
@Slf4j
public class NewAnalysisMessageProcessor extends AbstractMessageProcessor {


    private static final int MAX_PROCESS_TASK_NUM = 3;

    public NewAnalysisMessageProcessor(AnalysisApplication analysisApplication, RedisTaskAccessor redisTaskAccessor, String eventPattern) {
        super(analysisApplication, redisTaskAccessor, eventPattern);
    }

    @Override
    public void accept(Message message) {
        while (true) {
            boolean continueFlag = false;
//        选择任意一个任务将其从等待队列转移到处理队列
            Task task = takeTaskByType(TaskType.DOWNLOAD_AND_SOURCE_ANALYSIS);
            if (task != null) {
                int size = ((AnalysisApplication) application).runningAnalyst.size();
                if (size < MAX_PROCESS_TASK_NUM) {
                    continueFlag = true;
                    process0(task, message);
                }
            }
            if (!continueFlag) {
                break;
            }
        }
    }
}
