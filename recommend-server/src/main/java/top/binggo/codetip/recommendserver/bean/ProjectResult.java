package top.binggo.codetip.recommendserver.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author binggo
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProjectResult {

    private String id;

    private String groupId;

    private String artifactId;

    private String version;

    private String description;

    private String fileLocation;
}
