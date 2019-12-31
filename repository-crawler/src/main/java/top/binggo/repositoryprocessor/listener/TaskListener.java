package top.binggo.repositoryprocessor.listener;

import codetip.commons.bean.CrawlerTaskStatus;
import codetip.commons.crawl.RedisTaskAccessor;
import org.assertj.core.util.Lists;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.Topic;
import org.springframework.stereotype.Component;
import top.binggo.repositoryprocessor.analysis.AnalysisApplication;
import top.binggo.repositoryprocessor.crawler.CrawlerApplication;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用来监听Redis中关于Task的事件
 *
 * @author binggo
 */
@Component
public class TaskListener implements MessageListener {
    private Map<Topic, List<AbstractMessageProcessor>> messageProcessorMap = new ConcurrentHashMap<>(32);

    public TaskListener(RedisTaskAccessor redisTaskAccessor, CrawlerApplication crawlerApplication, AnalysisApplication analysisApplication) {
        NewCrawlMessageProcessor newCrawlMessageProcessor = new NewCrawlMessageProcessor(crawlerApplication, redisTaskAccessor, CrawlerTaskStatus.UN_START.getRedisKey());
        NewAnalysisMessageProcessor newAnalysisMessageProcessor = new NewAnalysisMessageProcessor(analysisApplication, redisTaskAccessor, CrawlerTaskStatus.UN_START.getRedisKey());
        StopCrawlMessageProcessor stopCrawlMessageProcessor = new StopCrawlMessageProcessor(crawlerApplication, redisTaskAccessor, CrawlerTaskStatus.PROCESSING.getRedisKey());
        messageProcessorMap
                .put(new ChannelTopic(newCrawlMessageProcessor.eventPattern), Lists.newArrayList(newCrawlMessageProcessor, newAnalysisMessageProcessor));
        messageProcessorMap.put(new ChannelTopic(stopCrawlMessageProcessor.eventPattern), Lists.newArrayList(stopCrawlMessageProcessor));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (pattern != null) {
            String string = new String(pattern);
            messageProcessorMap.forEach((key, processor) -> {
                if (key.getTopic().equals(string)) {
                    for (AbstractMessageProcessor abstractMessageProcessor : processor) {
                        abstractMessageProcessor.accept(message);
                    }
                }
            });
        }
    }

    public Collection<Topic> getTopics() {
        return messageProcessorMap.keySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaskListener that = (TaskListener) o;
        return Objects.equals(messageProcessorMap, that.messageProcessorMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageProcessorMap);
    }
}
