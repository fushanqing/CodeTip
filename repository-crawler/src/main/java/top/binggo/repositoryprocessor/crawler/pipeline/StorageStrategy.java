package top.binggo.repositoryprocessor.crawler.pipeline;

import java.io.IOException;

/**
 * @author binggo
 */
public interface StorageStrategy {
    boolean enable();

    void setEnable(boolean enable);

    String getTargetInfo();

    void sink(String fileName, String content) throws IOException;
}
