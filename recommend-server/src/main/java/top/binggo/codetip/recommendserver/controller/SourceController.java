package top.binggo.codetip.recommendserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.binggo.codetip.recommendserver.bean.Params;
import top.binggo.codetip.recommendserver.config.AppConfig;

/**
 * @author binggo
 */
@RestController
@Slf4j
public class SourceController {

    private final
    PreBuiltTransportClient preBuiltTransportClient;

    private final
    AppConfig appConfig;

    @Autowired
    public SourceController(PreBuiltTransportClient preBuiltTransportClient, AppConfig appConfig) {
        this.preBuiltTransportClient = preBuiltTransportClient;
        this.appConfig = appConfig;
    }

    @PostMapping("/source")
    public ResponseEntity<String> source(@RequestBody Params params) {
        GetRequestBuilder getRequestBuilder = preBuiltTransportClient.prepareGet(appConfig.getIndexName(), appConfig.getIndexType(), params.getIndexId());
        GetResponse source = getRequestBuilder.setFetchSource("source", null).get();
        String ret = (String) source.getSource().get("source");
        return ResponseEntity.ok(ret);
    }
}