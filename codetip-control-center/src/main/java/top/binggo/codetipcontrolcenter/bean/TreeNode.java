package top.binggo.codetipcontrolcenter.bean;

import lombok.*;
import top.binggo.codetip.filesystem.core.MavenRedisFileSystemNodeMetadata;

/**
 * @author binggo
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TreeNode {
    private String name;

    /**
     * <note>
     * nodeMetadata中不要包含链接信息
     * </note>
     */
    private MavenRedisFileSystemNodeMetadata nodeMetadata;
    private String path;

    public void setPath(String path) {
        int indexOf = path.lastIndexOf("/");
        this.name = indexOf == -1 ? path : path.substring(indexOf + 1);
        this.path = path;
    }
}


