package top.binggo.repositoryprocessor.crawler.subprocessor;

import lombok.extern.slf4j.Slf4j;
import top.binggo.repositoryprocessor.crawler.pipeline.MavenMetaDataPipeline;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.utils.HttpConstant;

import java.nio.charset.Charset;

/**
 * @see MavenMetaDataPipeline 存储完成的结构将放入这里处理
 * @author binggo
 */
@Slf4j
public class DownloadUrlProcessor extends AbstractSubProcessor {
    private static final String URL_PREFIX = "http://archiva-maven-storage-prod.oss-cn-beijing.aliyuncs.com/repository/central/";

    public DownloadUrlProcessor() {
        super(URL_PREFIX, "(.*)/(.*)\\?Expires=(.*)&OSSAccessKeyId=(.*)&Signature=(.*)");
    }

    @Override
    public void process(Page page) {
        if (page.getStatusCode() == HttpConstant.StatusCode.CODE_200 && page.getUrl().regex(urlRegex).match()) {
            log.info("downloadSources xml file by url:{}", page.getUrl());
            byte[] bytes = page.getBytes();
            //存储下载完成二进制结果转化成String
            log.debug(new String(bytes, Charset.forName("utf-8")));
            page.putField(page.getUrl().regex(getUrlRegex(), 1).get(), new String(bytes));
        } else {
            page.setSkip(true);
        }
    }
}