package top.binggo.repositoryprocessor.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

/**
 * @author binggo
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "process-result1", type = "process-result1", refreshInterval = "-1")
public class ProcessResult {
    @Id
    private String location;
    @Field
    private String description;
}
