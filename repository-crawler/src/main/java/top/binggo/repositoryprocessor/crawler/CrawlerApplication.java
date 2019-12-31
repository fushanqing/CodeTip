package top.binggo.repositoryprocessor.crawler;

import codetip.commons.bean.CrawlerTaskStatus;
import codetip.commons.crawl.RedisTaskAccessor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import top.binggo.codetip.filesystem.core.MavenRedisFileSystemAccessor;
import top.binggo.repositoryprocessor.crawler.pipeline.MavenMetaDataPipeline;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.downloader.HttpClientRequestContext;
import us.codecraft.webmagic.downloader.HttpUriRequestConverter;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.scheduler.RedisScheduler;
import us.codecraft.webmagic.scheduler.Scheduler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.BiFunction;

/**
 * 该类用于在应用程序中创建爬虫,运行爬虫任务的
 *
 * @author <a href=mailto:libing22@meituan.com>binggo</a>
 * @since 2019/3/14
 **/
@Component
@Slf4j
public class CrawlerApplication implements BiFunction<String, List<String>, String> {

    public static final String CRAWLER_APPLICATION_NAME = "Crawl/" + UUID.randomUUID().toString();
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 5;
    private Pipeline pipeline;
    private JedisPool jedisPool;
    private Scheduler scheduler;
    private Map<String, Spider> runningSpider;
    private SpiderMonitor spiderMonitor;
    private ExecutorService executorService;
    private RedisTaskAccessor redisTaskAccessor;

    @Autowired
    public CrawlerApplication(@Value("${spring.redis.host}") String redisHost, MavenRedisFileSystemAccessor mavenRedisFileSystemAccessor, RedisTaskAccessor redisTaskAccessor) {
        this.pipeline = new MavenMetaDataPipeline(mavenRedisFileSystemAccessor);
        this.redisTaskAccessor = redisTaskAccessor;
        jedisPool = new JedisPool(new GenericObjectPoolConfig(), redisHost);
        scheduler = new RedisScheduler(jedisPool);
        runningSpider = new ConcurrentHashMap<>(32);
        executorService = new ThreadPoolExecutor(CORE_POOL_SIZE
                , CORE_POOL_SIZE * 2
                , 1000, TimeUnit.MILLISECONDS
                , new LinkedBlockingDeque<>(1 << 8), new ThreadFactoryBuilder().setNameFormat("Spider-%d")
                .build(), (r, executor) -> log.warn("some task has bean abandoned")
        );
    }

    /**
     * 删除redis中存放的爬取历史记录
     */
    public void clearHistory() {
        try (Jedis resource = jedisPool.getResource()) {
            resource.select(0);
            Set<String> keys = resource.keys("*");
            resource.del(keys.toArray(new String[0]));
        } catch (Exception e) {
            log.error("{}", e);
        }
    }

    /**
     * 停止一个正在运行的爬虫任务
     *
     * @param taskId 要停止的任务的ID
     * @return 如果该任务被停止返回任务的taskId, 否则返回null
     */
    public String stopARunningCrawler(String taskId) {
        Spider spider = null;
        if ((spider = runningSpider.get(taskId)) != null) {
            spider.stop();
            runningSpider.remove(taskId);
            return taskId;
        }
        return null;
    }


    public Map<String, Spider> getRunningSpider() {
        return runningSpider;
    }

    @Override
    public String apply(String taskId, List<String> url) {
        HttpClientDownloader downloader = new HttpClientDownloader();
        downloader.setHttpUriRequestConverter(new CustomHttpUriRequestConverter());
        Spider spider = SpiderWithCloseHook.create(new MvnRepoPageProcessor(redisTaskAccessor, this, taskId), redisTaskAccessor, this)
                .setExecutorService(executorService).addPipeline(pipeline)
                .setDownloader(downloader)
                .setScheduler(scheduler);
        spider.setUUID(taskId);
        runningSpider.put(spider.getUUID(), spider);
        redisTaskAccessor.setTaskResultMsgInStatus(CrawlerTaskStatus.PROCESSING, taskId, CrawlerTaskStatus.PROCESSING.toString());
        spider.startUrls(url).start();
        return taskId;
    }

    private class CustomHttpUriRequestConverter extends HttpUriRequestConverter {
        @Override
        public HttpClientRequestContext convert(Request request, Site site, Proxy proxy) {
            request.addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
            HttpClientRequestContext convert = super.convert(request, site, proxy);
            return convert;
        }
    }
}
