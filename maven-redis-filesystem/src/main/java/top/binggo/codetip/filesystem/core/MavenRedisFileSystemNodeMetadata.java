package top.binggo.codetip.filesystem.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 文件系统中节点的属性信息
 *
 * @author followWindD
 */
@Data
public class MavenRedisFileSystemNodeMetadata {


    /**
     * 该节点的最后修改时间
     */
    private String lastModifiedTime = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    /**
     * 节点的类型
     * {@link NodeType}
     */
    private NodeType nodeType;
    /**
     * 如果一个节点是{@code nodeType==NodeType.FOLDER}就会有folderNodeStatus这个属性
     */
    private FolderNodeStatus folderNodeStatus;

    /**
     * 节点内容的连接
     */
    private ContentLink contentLink;

    private MavenRedisFileSystemNodeMetadata(NodeType nodeType, FolderNodeStatus folderNodeStatus, ContentLink contentLink) {
        setContentLink(contentLink);
        setFolderNodeStatus(folderNodeStatus);
        setNodeType(nodeType);
    }

    public MavenRedisFileSystemNodeMetadata() {

    }

    public static MavenRedisFileSystemNodeMetadata folder(String folderName) {
        return new MavenRedisFileSystemNodeMetadata(NodeType.FOLDER, new FolderNodeStatus(), ContentLink.folder(folderName));
    }

    public static MavenRedisFileSystemNodeMetadata sourcefolder(String folderName) {
        return new MavenRedisFileSystemNodeMetadata(NodeType.SOURCE_FOLDER, null, ContentLink.folder(folderName));
    }

    public static MavenRedisFileSystemNodeMetadata file(ContentLink contentLink) {
        return new MavenRedisFileSystemNodeMetadata(NodeType.FILE, null, contentLink);
    }

    public static MavenRedisFileSystemNodeMetadata xml(String xmlFileName) {
        return new MavenRedisFileSystemNodeMetadata(NodeType.XML, null, ContentLink.xml(xmlFileName));
    }

    public void refreshLastModifiedTime() {
        lastModifiedTime = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
        if (this.nodeType != NodeType.FOLDER) {
            folderNodeStatus = null;
        }
    }

    public void setFolderNodeStatus(FolderNodeStatus folderNodeStatus) {
        if (nodeType == NodeType.FOLDER) {
            this.folderNodeStatus = folderNodeStatus;
        }
    }


    /**
     * 表示节点类型
     */
    public enum NodeType {
        /**
         * 文件夹
         */
        FOLDER,
        /**
         * 包含源代码的文件夹
         */
        SOURCE_FOLDER,
        /**
         * XML
         */
        XML,
        /**
         * class压缩问价
         */
        CLASS,
        /**
         * 反编译形成的源代码
         */
        DECOMPILE_SOURCE,
        /**
         * 源代码文件
         */
        SOURCE,
        /**
         * 普通文件或者位置类型
         */
        FILE;

        public static boolean isFolderType(NodeType nodeType) {
            return nodeType == NodeType.FOLDER || nodeType == NodeType.SOURCE_FOLDER;
        }
    }

    /**
     * 表示节点类型
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class FolderNodeStatus {
        /**
         * 0 未完成
         * 1 已完成
         * -1 无法完成
         */
        private int allSourceDownloadStatus;
        private int allSourceAnalysisStatus;
        private int allUseAnalysisStatus;

        private static int getADependentB(int a, int b) {
            return b == 0 || b == -1 ? b : a;
        }

        public FolderNodeStatus deepCopy() {
            return new FolderNodeStatus(allSourceDownloadStatus, allSourceAnalysisStatus
                    , allUseAnalysisStatus);
        }

        public int getAllSourceAnalysisStatus() {
            return getADependentB(allSourceAnalysisStatus, allSourceDownloadStatus);
        }

        public int getAllUseAnalysisStatus() {
            return getADependentB(allUseAnalysisStatus, allSourceAnalysisStatus);
        }

        public FolderNodeStatus merge(FolderNodeStatus other) {
            return new FolderNodeStatus(Math.min(allSourceDownloadStatus, other.allSourceDownloadStatus),
                    Math.min(allSourceAnalysisStatus, other.allSourceAnalysisStatus),
                    Math.min(allUseAnalysisStatus, other.allUseAnalysisStatus));
        }

    }

    /**
     * 表示内容的连接
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ContentLink {
        static final String REDIS_KEY_PREFIX = "#content#";
        private String storedLocation;
        private TypeOfStorage typeOfStorage;


        public static ContentLink folder(String folderName) {
            folderName = preparePrefix(folderName);
            return new ContentLink(folderName, TypeOfStorage.REDIS_SET);
        }

        public static ContentLink xml(String xmlFileName) {
            xmlFileName = preparePrefix(xmlFileName);
            return new ContentLink(xmlFileName, TypeOfStorage.REDIS_STRING);
        }

        private static String preparePrefix(String ori) {
            if (!ori.startsWith(MavenRedisFileSystemAccessor.ROOT_DIR_NAME)) {
                ori = MavenRedisFileSystemAccessor.ROOT_DIR_NAME + "/" + ori;
            }
            if (!ori.startsWith(REDIS_KEY_PREFIX)) {
                ori = REDIS_KEY_PREFIX + ori;
            }
            return ori;
        }
    }
}
