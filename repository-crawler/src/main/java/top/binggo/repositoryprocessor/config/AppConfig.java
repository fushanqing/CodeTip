package top.binggo.repositoryprocessor.config;

import codetip.commons.bean.Task;
import codetip.commons.crawl.RedisTaskAccessor;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import top.binggo.codetip.filesystem.core.MavenRedisFileSystemAccessor;
import top.binggo.repositoryprocessor.listener.TaskListener;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author binggo
 */
@SpringBootConfiguration
@EnableElasticsearchRepositories
public class AppConfig {


    @Bean
    public static MavenRedisFileSystemAccessor mavenRedisFileSystemAccessor(StringRedisTemplate stringRedisTemplate) {
        return new MavenRedisFileSystemAccessor(stringRedisTemplate);
    }

    @Bean
    public static RedisMessageListenerContainer redisMessageListenerContainer(TaskListener taskListener, RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(2, 3, 10, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(1 << 8), new ThreadFactoryBuilder().setNameFormat("task-executor-%d").build());
        ThreadPoolExecutor subscriptionExecutor = new ThreadPoolExecutor(1, 2, 1, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(1 << 8), new ThreadFactoryBuilder().setNameFormat("subscription-executor-%d").build());
        redisMessageListenerContainer.setMessageListeners(ImmutableMap.of(taskListener, taskListener.getTopics()));
        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
        redisMessageListenerContainer.setSubscriptionExecutor(taskExecutor);
        redisMessageListenerContainer.setSubscriptionExecutor(subscriptionExecutor);
        return redisMessageListenerContainer;
    }

    @Bean
    @SuppressWarnings("unchecked")
    public static RedisTemplate jsonRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setHashValueSerializer(RedisSerializer.json());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(RedisSerializer.json());
        return redisTemplate;
    }

    @Bean
    public static RedisTaskAccessor redisTaskAccessor(RedisTemplate<String, Task> jsonRedisTemplate, StringRedisTemplate stringRedisTemplate) {
        return new RedisTaskAccessor(jsonRedisTemplate, stringRedisTemplate);
    }

    @Value("${spring.data.elasticsearch.cluster-name}")
    private String clusterName;

    @Value("${spring.data.elasticsearch.cluster-nodes}")
    private String address;

    @Value("${codeTip.es.recommend.indexName}")
    private String indexName;

    @Value("${codeTip.es.recommend.indexType}")
    private String indexType;

    public String getIndexName() {
        return indexName;
    }

    public String getIndexType() {
        return indexType;
    }

    @Bean
    public PreBuiltTransportClient preBuiltTransportClient() throws UnknownHostException {
        Settings setting = Settings.builder().put("cluster.name", this.clusterName).build();
        PreBuiltTransportClient preBuiltTransportClient = new PreBuiltTransportClient(setting);
        InetAddress address = InetAddress.getByName(
                this.address.substring(0, this.address.indexOf(':')));
        preBuiltTransportClient.addTransportAddress(new TransportAddress(address, 9300));
        return preBuiltTransportClient;
    }

    @Bean
    public BulkProcessor bulkProcessor(PreBuiltTransportClient preBuiltTransportClient) {

        return BulkProcessor.builder(preBuiltTransportClient,
                new BulkListener()
        ).setFlushInterval(TimeValue.timeValueSeconds(10))
                .setConcurrentRequests(4)
                .setBackoffPolicy(
                        BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(100), 3)).build();

    }

    @Slf4j
    private static class BulkListener implements BulkProcessor.Listener {

        @Override
        public void beforeBulk(long executionId, BulkRequest request) {
            log.info("beforeBulk={} actionNum={}", executionId, request.numberOfActions());
        }

        @Override
        public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
            log.info("afterBulk={} status={}", executionId, response.status());
        }

        @Override
        public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
            log.info("afterBulk executionId={} failure={}", executionId, failure);
        }
    }
}
