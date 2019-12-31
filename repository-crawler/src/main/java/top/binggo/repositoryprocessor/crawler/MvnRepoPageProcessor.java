package top.binggo.repositoryprocessor.crawler;

import codetip.commons.crawl.RedisTaskAccessor;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.binggo.repositoryprocessor.crawler.subprocessor.AbstractSubProcessor;
import top.binggo.repositoryprocessor.crawler.subprocessor.DownloadUrlProcessor;
import top.binggo.repositoryprocessor.crawler.subprocessor.FileInfoProcessor;
import top.binggo.repositoryprocessor.crawler.subprocessor.TreePathProcessor;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.utils.HttpConstant;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href=mailto:libing22@meituan.com>binggo</a>
 * @since 2019/3/1
 **/
@ThreadSafe
public class MvnRepoPageProcessor implements PageProcessor {
	private static Logger logger = LoggerFactory.getLogger(MvnRepoPageProcessor.class);
	/**
	 * https://maven.aliyun.com/mvn/view
	 * 上面的连接是要爬去的页面,主要是去解析 ajax
	 * 的 get 请求返回的 json 结果
	 */
	private Site site = Site.me().setRetryTimes(3).setSleepTime(1000).setCharset("utf-8");
	private Map<String, AbstractSubProcessor> regex2Processor = new HashMap<>();
	public final String targetUrl = "https://maven.aliyun.com/browse/tree?_input_charset=utf-8&repoId=central&path=";
    private static final AbstractSubProcessor FILE_INFO_PROCESSOR = new FileInfoProcessor();
    private static final AbstractSubProcessor DOWNLOAD_URL_PROCESSOR = new DownloadUrlProcessor();

    public MvnRepoPageProcessor(RedisTaskAccessor redisTaskAccessor, CrawlerApplication crawlerApplication, String taskId) {
		//注册特别的SubProcessor到regex2Processor
        AbstractSubProcessor[] processors = {new TreePathProcessor(redisTaskAccessor, crawlerApplication, taskId), FILE_INFO_PROCESSOR, DOWNLOAD_URL_PROCESSOR};
		for (AbstractSubProcessor processor : processors) {
			regex2Processor.put(processor.getUrlRegex(), processor);
		}
	}


	@Override
	public void process(Page page) {

		//page的返回结果是否符合预期
		if (page.getStatusCode() != HttpConstant.StatusCode.CODE_200) {
			logger.warn("url:{} can't be process because status-code is {}", page.getUrl(), page.getStatusCode());
			return;
		}
		//根基 url 结果选择处理器
		final List<AbstractSubProcessor> targetProcessors = new ArrayList<>();
		regex2Processor.forEach((k, v) -> {
			if (page.getUrl().regex(k).match()) {
				targetProcessors.add(v);
			}
		});
        Preconditions.checkState(targetProcessors.size() == 1,
				"targetProcessors.size() == %s,but it should be 1",
				targetProcessors.size());
		//将具体处理的过程交给targetProcessors.get(0)处理
		targetProcessors.get(0).process(page);
	}

	@Override
	public Site getSite() {
		return this.site;
	}


}
