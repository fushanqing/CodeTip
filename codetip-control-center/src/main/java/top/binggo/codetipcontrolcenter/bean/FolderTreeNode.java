package top.binggo.codetipcontrolcenter.bean;

import top.binggo.codetip.filesystem.core.MavenRedisFileSystemNodeMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * @author binggo
 */
public class FolderTreeNode extends TreeNode {
    private List<TreeNode> children = new ArrayList<>(32);

    public FolderTreeNode() {
    }

    public FolderTreeNode(String path, MavenRedisFileSystemNodeMetadata nodeMetadata) {
        super();
        this.setPath(path);
        this.setNodeMetadata(nodeMetadata);
    }

    public List<TreeNode> getChildren() {
        return children;
    }
}
