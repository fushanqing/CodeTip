package top.binggo.repositoryprocessor.analysis;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;

/**
 * 从阿里云中央镜像仓库
 *
 * @author binggo
 */
@Slf4j
public class AliMavenRepoCentralJavaSourceDownloader implements JavaSourceDownloader {
    private static final String URL_PREFIX = "https://maven.aliyun.com/nexus/content/repositories/central/";

    @Override
    public InputStream download(String groupId, String artifactId, String version, String fileType) throws IOException {
        String sourceFileName = artifactId + "-" + version + fileType;
        String urlString = URL_PREFIX + groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version + "/" + sourceFileName;
        log.info("download url={}", urlString);
        HttpClient httpclient = HttpClients.createDefault();
        HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy());
        HttpGet request = new HttpGet(urlString);
        HttpResponse response = httpclient.execute(request);
        if (response.getStatusLine().getStatusCode() == 200) {
            log.info("download url={} success", urlString);
            return response.getEntity().getContent();
        }
        log.info("download url={} fail", urlString);
        return null;
    }


}
