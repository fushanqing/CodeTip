package top.binggo.repositoryprocessor.analysis.sink;

import codetip.commons.bean.MemberIndex;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Repository;
import top.binggo.repositoryprocessor.config.AppConfig;

import java.util.List;

/**
 * @author binggo
 */
@Repository
public class MemberIndexBulkRepository {

    public final
    BulkProcessor bulkProcessor;

    final
    AppConfig appConfig;

    public MemberIndexBulkRepository(BulkProcessor bulkProcessor, AppConfig appConfig) {
        this.bulkProcessor = bulkProcessor;
        this.appConfig = appConfig;
    }

    public void bulkIndex(List<MemberIndex> memberIndices) throws JsonProcessingException {
        if (memberIndices != null) {
            ObjectMapper mapper = new ObjectMapper();
            for (MemberIndex memberIndex : memberIndices) {
                byte[] bytes = mapper.writeValueAsBytes(memberIndex);
                IndexRequest request = new IndexRequest(appConfig.getIndexName(), appConfig.getIndexType(), memberIndex.getIndexId().substring(0,Math.min(512,memberIndex.getIndexId().length())));
                request.source(bytes, XContentType.JSON);
                this.bulkProcessor.add(request);
            }
        }
    }

    public void flush() {
        bulkProcessor.flush();
    }

    public void close() {
        bulkProcessor.close();
    }
}
