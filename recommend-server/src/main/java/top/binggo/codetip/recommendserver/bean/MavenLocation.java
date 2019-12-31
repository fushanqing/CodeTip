package top.binggo.codetip.recommendserver.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author binggo
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MavenLocation {
    private String groupId;

    private String artifactId;

    private String version;

    public boolean isEmpty() {
        return groupId == null || groupId.isEmpty() && artifactId.isEmpty() && version.isEmpty();
    }
}
