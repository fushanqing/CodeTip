package top.binggo.repositoryprocessor.crawler;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import top.binggo.codetip.filesystem.core.MavenRedisFileSystemAccessor;

/**
 * 一个用来测试爬虫的接口
 *
 * @author binggo
 */
@RestController
@Slf4j
public class AccessorController {

    private final CrawlerApplication crawlerApplication;


    private final MavenRedisFileSystemAccessor mavenRedisFileSystemAccessor;

    @Autowired
    public AccessorController(CrawlerApplication crawlerApplication, MavenRedisFileSystemAccessor mavenRedisFileSystemAccessor) {
        this.crawlerApplication = crawlerApplication;
        this.mavenRedisFileSystemAccessor = mavenRedisFileSystemAccessor;
    }


    @GetMapping("/test/{newTest}")
    public ResponseEntity<String> test(@PathVariable String newTest) {
        String s =
//                "https://maven.aliyun.com/browse/tree?_input_charset=utf-8&repoId=central&path=com%2Falibaba%2FSimpleEL%2F";
                "https://maven.aliyun.com/browse/tree?_input_charset=utf-8&repoId=central&path=org/springframework/";
        String clearFlag = "1";
        if (clearFlag.equals(newTest)) {
            log.info("clear history and origin file system");
            crawlerApplication.clearHistory();
            mavenRedisFileSystemAccessor.clearFileSystem();
        }
        return ResponseEntity.ok(crawlerApplication.apply("test", Lists.newArrayList(s)));
    }

}
