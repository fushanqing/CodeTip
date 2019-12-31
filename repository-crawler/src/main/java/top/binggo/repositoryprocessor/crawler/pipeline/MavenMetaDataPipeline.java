package top.binggo.repositoryprocessor.crawler.pipeline;

import codetip.commons.bean.MavenMetadataPublicBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.binggo.codetip.filesystem.core.MavenMetadataReader;
import top.binggo.codetip.filesystem.core.MavenRedisFileSystemAccessor;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * 用来根据已经获得的maven-metadata.xml字节流进行分析
 *
 * @author binggo
 */
@Component
@Slf4j
public class MavenMetaDataPipeline implements Pipeline, BiConsumer<String, Object> {

    private static final String METADATA_FILE_NAME = "maven-metadata-public.xml";
    private MavenRedisFileSystemAccessor mavenRedisFileSystemAccessor;

    @Autowired
    public MavenMetaDataPipeline(MavenRedisFileSystemAccessor mavenRedisFileSystemAccessor) {
        this.mavenRedisFileSystemAccessor = mavenRedisFileSystemAccessor;
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        resultItems.getAll().forEach(this);
    }

    @Override
    public void accept(String s, Object o) {
        log.info("build path for file:{}", s);
        String content = (String) o;
        String storageFileName = s + "/" + METADATA_FILE_NAME;
        MavenMetadataPublicBean mavenMetadataPublicBean = null;
        try {
            log.info("content:{}", content);
            mavenMetadataPublicBean = MavenMetadataReader.getMavenMetadataPublicBean(content);
        } catch (IOException e) {
            log.error("{}", e);
            throw new PipelineBlockException(e);
        }
        //不处理无用项
        if (mavenMetadataPublicBean == null || mavenMetadataPublicBean.isUseless()) {
            return;
        }

        MavenMetadataPublicBean.Versioning versioning = mavenMetadataPublicBean.getVersioning();
        //没有最后更新时间戳，时间戳过期
        if (versioning == null || versioning.getLastUpdated() == null || storageStatus(storageFileName, versioning.getLastUpdated()) != 1) {
            mavenRedisFileSystemAccessor.newXml(s, METADATA_FILE_NAME, content);
            //如果推荐版本目录还未创建,创建推荐目录
            String recommendVersion = mavenMetadataPublicBean.getRecommendVersion();
            mavenRedisFileSystemAccessor.newFolder(s, recommendVersion);
            //刷新各个节点状态
            String newFolderName = s + recommendVersion;
            try {
                mavenRedisFileSystemAccessor.refreshFolderNodeStatus(newFolderName, null);
            } catch (JsonProcessingException e) {
                log.error("base on folder:{} status,refresh file node tree fail.cause by {}", newFolderName, e);
            }
        }
    }


    /**
     * -1 不存在
     * 0 存在，但过期
     * 1 存在，未过期
     * 判断在redis中是否已经存储了相应的xml文件，并比较新的修改时间是否比原先文件的修改时间要滞后
     */
    private int storageStatus(String fileName, String lastUpdated) {
        String content = mavenRedisFileSystemAccessor.getRedisContentAccessor().valueGet(fileName);
        if (!StringUtils.isBlank(content)) {
            XmlMapper xmlMapper = new XmlMapper();
            try {
                MavenMetadataPublicBean mavenMetadataPublicBean = xmlMapper.readValue(content, MavenMetadataPublicBean.class);
                String lastUpdated1 = mavenMetadataPublicBean.getVersioning().getLastUpdated();
                return compareDayFormatString(lastUpdated, lastUpdated1) > 0 ? 0 : 1;
            } catch (IOException e) {
                log.error("{}", e);
                return 0;
            }
        }
        return -1;
    }

    /**
     * a>b => 1
     * a=b => 0
     * a<b => 1
     */
    private int compareDayFormatString(String a, String b) {
        ImmutableList<Character> skipString = ImmutableList.of('-', ' ', '_');
        int i = 0, j = 0;
        while (i < a.length() && j < b.length()) {
            char aChar = a.charAt(i);
            char bChar = b.charAt(j);
            if (skipString.contains(aChar)) {
                i++;
                continue;
            }
            if (skipString.contains(bChar)) {
                j++;
                continue;
            }
            if (aChar == bChar) {
                i++;
                j++;
            } else {
                return (aChar > bChar) ? 1 : -1;
            }
        }
        return 0;
    }


    private static class PipelineBlockException extends RuntimeException {
        public PipelineBlockException() {
            super();
        }

        public PipelineBlockException(Throwable cause) {
            super(cause);
        }
    }
}

