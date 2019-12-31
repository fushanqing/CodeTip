package top.binggo.codetipcontrolcenter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.binggo.codetip.filesystem.core.MavenRedisFileSystemAccessor;
import top.binggo.codetip.filesystem.core.MavenRedisFileSystemNodeMetadata;
import top.binggo.codetipcontrolcenter.bean.FolderTreeNode;
import top.binggo.codetipcontrolcenter.bean.TreeNode;

import java.util.Map;

/**
 * @author binggo
 */
@Service
public class FileTreeService {


    private final MavenRedisFileSystemAccessor mavenRedisFileSystemAccessor;

    @Autowired
    public FileTreeService(MavenRedisFileSystemAccessor mavenRedisFileSystemAccessor) {
        this.mavenRedisFileSystemAccessor = mavenRedisFileSystemAccessor;
    }

    public TreeNode listFolder(String folderName, boolean withFolderStatus) {
        Map<String, MavenRedisFileSystemNodeMetadata> treeNodes = mavenRedisFileSystemAccessor.listFolder(folderName);
        if (treeNodes == null) {
            return null;
        }
        MavenRedisFileSystemNodeMetadata metadata = null;
        if (withFolderStatus) {
            metadata = mavenRedisFileSystemAccessor.getMetadataAccessor().getMetadata(folderName);
            metadata.setContentLink(null);
        }
        FolderTreeNode ret = new FolderTreeNode(folderName, metadata);
        treeNodes.forEach((key, value) -> {
//            隐藏内容的连接信息
            value.setContentLink(null);
            TreeNode build;
            if (MavenRedisFileSystemNodeMetadata.NodeType.isFolderType(value.getNodeType())) {
                build = new FolderTreeNode(key, value);
            } else {
                build = TreeNode.builder().nodeMetadata(value).build();
                build.setPath(key);
            }
            ret.getChildren().add(build);
        });
        return ret;
    }
}
