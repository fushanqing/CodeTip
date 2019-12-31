package codetip.commons.bean;

import codetip.commons.crawl.RedisTaskAccessor;

/**
 * @author binggo
 */
public enum CrawlerTaskStatus {

//todo 去掉 STOPPED状态
    /**
     * 已经完成
     */
    FINISHED,
    /**
     * 中断的
     */
    INTERRUPTED,
    /**
     * 正在处理
     */
    PROCESSING,
    /**
     * 停止的
     */
    STOPPED,
    /**
     * 还未启动
     */
    UN_START;


    public boolean isProcessing() {
        return this == PROCESSING;
    }

    public String getRedisKey() {
        return RedisTaskAccessor.ACCESS_KEY + toString();
    }
}
