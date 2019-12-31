package codetip.commons.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.Set;

/**
 * @author binggo
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Task {
    private String taskId;
    private String createTime;
    private String lastModifiedTime;
    private String operator;
    private CrawlerTaskStatus status;
    private Set<String> content;
    private TaskType taskType;
    private String resultMsg;

    @Override
    public boolean equals(Object o) {
        if (o instanceof Task) {
            return Objects.equals(taskId, ((Task) o).taskId);
        }
        return false;
    }
}
