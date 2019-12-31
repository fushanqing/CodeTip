package top.binggo.repositoryprocessor.analysis;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author followWindD
 */
public interface JavaSourceDownloader {
    /**
     * 下载maven坐标对应的javaSource
     *
     * @param groupId    maven坐标中的groupId
     * @param artifactId maven坐标中的artifactId
     * @param version    maven坐标中的version
     * @return 存储javadoc流
     */
    InputStream download(String groupId, String artifactId, String version, String fileType) throws IOException;
}
