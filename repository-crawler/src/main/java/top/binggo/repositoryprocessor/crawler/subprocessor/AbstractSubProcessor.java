package top.binggo.repositoryprocessor.crawler.subprocessor;

import lombok.NonNull;
import us.codecraft.webmagic.Page;

/**
 * @author binggo
 */
public abstract class AbstractSubProcessor {
	/**
	 * 当前SubProcessor要处理的请求路径的正则表达式
	 * 不同SubProcessor对象的urlRegex要没有交集
	 */
	protected final String urlRegex;

    public AbstractSubProcessor(@NonNull String urlPrefix, @NonNull String pattern) {
        this.urlRegex = urlPrefix.replace(".", "\\.").replace("?", "\\?") + pattern;
	}

	public String getUrlRegex() {
		return urlRegex;
	}

	/**
	 * 页面的处理逻辑
	 *
	 * @param page 一次请求的返回结果,可以是一个 html 页面也可能是
	 *             json
	 */
	public abstract void process(Page page);
}