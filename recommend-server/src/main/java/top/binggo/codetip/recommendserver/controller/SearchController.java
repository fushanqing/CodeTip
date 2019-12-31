package top.binggo.codetip.recommendserver.controller;

import codetip.commons.bean.MemberIndex;
import codetip.commons.bean.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.binggo.codetip.recommendserver.bean.MavenLocation;
import top.binggo.codetip.recommendserver.service.SearchService;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author binggo
 */
@RestController
@RequestMapping("/search")
@Slf4j
public class SearchController {
    private final
    SearchService searchService;

    //todo 添加用户设置自己的pom文件接口，并在获得对应pom文件时，能够爬取对应目录和分析对应源代码
    //todo 添加仓库使用情况统计分析
    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/project")
    public ResponseEntity<Page.Respond<MemberIndex>> searchProject(Page.Request page, @RequestParam(required = false) String keyWords, @RequestParam(required = false, defaultValue = "false") boolean onlyRecommendVersion) {
        log.info("SearchController.searchProject");
        return ResponseEntity.ok(searchService.searchProject(page, keyWords, onlyRecommendVersion));
    }

    @GetMapping("/class")
    public ResponseEntity<Page.Respond<MemberIndex>> searchClass(Page.Request page, @RequestParam(required = false) String groupId, @RequestParam(required = false) String artifactId, @RequestParam(required = false) String version, @RequestParam(required = false) String keyWords, @RequestParam(required = false, defaultValue = "false") boolean onlyPublicMember) {
        log.info("SearchController.searchClass");
        MavenLocation mavenLocation = new MavenLocation(groupId, artifactId, version);
        return ResponseEntity.ok(searchService.searchClass(page, mavenLocation, keyWords, onlyPublicMember));
    }

    @GetMapping("/member")
    public ResponseEntity<Page.Respond<MemberIndex>> searchMember(Page.Request page, @RequestParam(required = false) String groupId, @RequestParam(required = false) String artifactId, @RequestParam(required = false) String version, @RequestParam(required = false) String className, @RequestParam(required = false) String keyWords, @RequestParam(required = false, defaultValue = "false") boolean filterOverloadMethod, @RequestParam(required = false, defaultValue = "false") boolean onlyPublicMember) {
        log.info("SearchController.searchMember");
        MavenLocation mavenLocation = new MavenLocation(groupId, artifactId, version);
        return ResponseEntity.ok(searchService.searchMember(page, mavenLocation, className, keyWords, filterOverloadMethod, onlyPublicMember));
    }


    @GetMapping("/fontPage")
    public ResponseEntity frontPage(String keyWords) throws URISyntaxException {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI("http://localhost:8080/frontPage?searchKeyWord=" + keyWords));
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).headers(headers).build();
    }


}
