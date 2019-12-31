package top.binggo.codetip.filesystem.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用来构造maven坐标的树形结构的文件系统
 * <p>
 * 使用redis中的set结构维护文件系统的树状结构
 * key为节点的全路径名称,set中的值为节点的名称
 * </p>
 * <p>
 * 节点的属性信息使用redis的hash结构存储
 * 大键为{@code MavenTreeAccessing.REDIS_KEY_PREFIX}+节点全路径名称
 * 小键为属性的名称
 * <note>
 * 文件系统路径格式:aaa/bbb/ccc
 * </note>
 * </p>
 *
 * @author followWindD
 */
@Slf4j
@ThreadSafe
public class MavenRedisFileSystemAccessor {
    public static final String ROOT_DIR_NAME = "#root";
    private static final String FOLDER_SEGMENTATION = "/";
    private StringRedisTemplate stringRedisTemplate;
    private MetadataAccessor metadataAccessor = new MetadataAccessor();
    private RedisContentAccessor redisContentAccessor = new RedisContentAccessor();


    @Autowired
    public MavenRedisFileSystemAccessor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        //如果开始时没有创建根目录,则创建更目录root
        if (metadataAccessor.getMetadata(ROOT_DIR_NAME) == null) {
            try {
                metadataAccessor.setNodeMetaDataInRedis(ROOT_DIR_NAME, MavenRedisFileSystemNodeMetadata.folder(ROOT_DIR_NAME));
            } catch (JsonProcessingException e) {
                throw new CreationFailedException(e.getMessage());
            }
        }
    }

    public static String absoluteFolderName2Url(String absoluteFolderName) {
        String prefix = ROOT_DIR_NAME + FOLDER_SEGMENTATION;
        if (absoluteFolderName.startsWith(prefix)) {
            absoluteFolderName = absoluteFolderName.substring(prefix.length());
        }
        if (!absoluteFolderName.endsWith(FOLDER_SEGMENTATION)) {
            absoluteFolderName += FOLDER_SEGMENTATION;
        }
        return "https://maven.aliyun.com/browse/tree?_input_charset=utf-8&repoId=central&path=" + absoluteFolderName;
    }

    /**
     * 如果访问的时候没有加上root前缀,则加上
     */
    private static String getAbsolutePath(String path) {
        if (!path.startsWith(ROOT_DIR_NAME)) {
            String prefix = path.startsWith(FOLDER_SEGMENTATION) ? ROOT_DIR_NAME : ROOT_DIR_NAME + FOLDER_SEGMENTATION;
            path = prefix + path;
        }
        return path;
    }

    private static Pair<String, String> pre(String currentLocation) {
        int indexOf = currentLocation.lastIndexOf(FOLDER_SEGMENTATION);
        if (indexOf == -1) {
            return Pair.of("", currentLocation);
        }
        return Pair.of(currentLocation.substring(0, indexOf), currentLocation.substring(indexOf + 1));
    }

    public MetadataAccessor getMetadataAccessor() {
        return metadataAccessor;
    }

    public RedisContentAccessor getRedisContentAccessor() {
        return redisContentAccessor;
    }

    /**
     * 创建子文件夹
     */
    public boolean newFolder(String parentNodeName, String nodeName) {
        //创建目录结构和属性文件
        String newFolderName = parentNodeName + FOLDER_SEGMENTATION + nodeName;
        MavenRedisFileSystemNodeMetadata folder = MavenRedisFileSystemNodeMetadata.folder(newFolderName);
        return newNodeP(parentNodeName, nodeName, folder);
    }

    public boolean newXml(String parentNodeName, String nodeName, String content) {
        //创建目录结构和属性文件
        String xmlFileName = parentNodeName + FOLDER_SEGMENTATION + nodeName;
        boolean b = newNodeP(parentNodeName, nodeName, MavenRedisFileSystemNodeMetadata.xml(xmlFileName));
        //放入内容
        if (b && content != null) {
            redisContentAccessor.valueAdd(xmlFileName, content);
        }
        return b;
    }

    public boolean newFile(String parentNodeName, String nodeName, MavenRedisFileSystemNodeMetadata.ContentLink contentLink) {
        return newNodeP(parentNodeName, nodeName, MavenRedisFileSystemNodeMetadata.file(contentLink));
    }

    /**
     * @param foldAbsoluteName 要list的文件夹的名称
     * @return 文件夹中的内容, 如果为null这表示无法打开这个文件夹(不存在 / 不是文件夹)
     */
    public Map<String, MavenRedisFileSystemNodeMetadata> listFolder(String foldAbsoluteName) {
        foldAbsoluteName = getAbsolutePath(foldAbsoluteName);
        if (null == isFolder(foldAbsoluteName)) {
            return null;
        }
        Set<String> listFileNames = redisContentAccessor.setGet(foldAbsoluteName);

        if (listFileNames == null) {
            return null;
        }
        Map<String, MavenRedisFileSystemNodeMetadata> ret = new LinkedHashMap<>(32);
        listFileNames.forEach(
                v -> {
                    MavenRedisFileSystemNodeMetadata metadata1 = metadataAccessor.getMetadata(v);
                    if (metadata1 != null) {
                        ret.put(v, metadata1);
                    }
                }
        );
        return ret;
    }

    /**
     * 根据最开始文件夹状态的变更刷新他的上一层文件夹
     *
     * @param folderName 最开始的文件夹
     */
    public void refreshFolderNodeStatus(String folderName, MavenRedisFileSystemNodeMetadata.FolderNodeStatus childFolderNodeStatus) throws JsonProcessingException {
        MavenRedisFileSystemNodeMetadata folder = isFolder(folderName);
        //如果目录是文件夹
        if (folder != null) {
            MavenRedisFileSystemNodeMetadata.FolderNodeStatus folderNodeStatus = folder.getFolderNodeStatus();
            MavenRedisFileSystemNodeMetadata.FolderNodeStatus copy = folderNodeStatus.deepCopy();
            boolean continueFlag = childFolderNodeStatus == null;
            if (childFolderNodeStatus != null && !folderNodeStatus.equals(childFolderNodeStatus)) {
                folderNodeStatus = folderNodeStatus.merge(childFolderNodeStatus);
                if (!copy.equals(folderNodeStatus)) {
                    folder.setFolderNodeStatus(folderNodeStatus);
                    metadataAccessor.setNodeMetaDataInRedis(folderName, folder);
                    continueFlag = true;
                }
            }
            Pair<String, String> pre = pre(folderName);
            if (!StringUtils.isBlank(pre.getLeft()) && continueFlag) {
                refreshFolderNodeStatus(pre.getLeft(), folderNodeStatus);
            }
        }

    }

    /**
     * 清除文件系统
     */
    public void clearFileSystem() {
        Set<String> keys = stringRedisTemplate.keys("*");
        if (!CollectionUtils.isEmpty(keys)) {
            keys.remove("#metadata##root");
            stringRedisTemplate.delete(keys);
        }
    }

    public MavenRedisFileSystemNodeMetadata isFolder(String foldAbsoluteName) {
        foldAbsoluteName = getAbsolutePath(foldAbsoluteName);
        MavenRedisFileSystemNodeMetadata metadata = metadataAccessor.getMetadata(foldAbsoluteName);
        if (metadata == null || !MavenRedisFileSystemNodeMetadata.NodeType.isFolderType(metadata.getNodeType())) {
            return null;
        }
        return metadata;
    }

    private boolean newNodeP(String parentNodeName, String nodeName, MavenRedisFileSystemNodeMetadata mavenRedisFileSystemNodeMetadata) {
        parentNodeName = getAbsolutePath(parentNodeName);
        return newNodeP0(parentNodeName, nodeName, mavenRedisFileSystemNodeMetadata);
    }

    /**
     * 递归的创建一个文件(如果父文件夹不存在则递归创建父文件夹)
     *
     * @param parentNodeName                   父节点的全名
     * @param nodeName                         当前节点全名
     * @param mavenRedisFileSystemNodeMetadata node节点属性信息
     * @return 是否创建成功
     */
    private boolean newNodeP0(String parentNodeName, String nodeName, MavenRedisFileSystemNodeMetadata mavenRedisFileSystemNodeMetadata) {
        //检查文件的名称是否带有非法字符‘/’，因为会使用该字符分隔父文件和子文件
        if (nodeName.contains(FOLDER_SEGMENTATION)) {
            return false;
        }
        //检查父文件时候存在
        if (!createNodePrecondition(parentNodeName)) {
            //不存在，通过绝对路径获得父文件的名称
            Pair<String, String> pre = pre(parentNodeName);
            //创建父文件
            boolean b = newNodeP0(pre.getLeft(), pre.getRight(), MavenRedisFileSystemNodeMetadata.folder(parentNodeName));
            if (!b || parentNodeName.equals("")) {
                return false;
            }
        }
        //创建当前文件
        return newNode(parentNodeName, nodeName, mavenRedisFileSystemNodeMetadata);

    }

    private boolean newNode(String parentNodeName, String nodeName, MavenRedisFileSystemNodeMetadata mavenRedisFileSystemNodeMetadata) {
        try {
            metadataAccessor.setNodeMetaDataInRedis(parentNodeName + FOLDER_SEGMENTATION + nodeName, mavenRedisFileSystemNodeMetadata);
            redisContentAccessor.setAdd(parentNodeName, nodeName);
        } catch (JsonProcessingException e) {
            log.error("create metadata for node:{} ,caused {}", nodeName, e);
            return false;
        }
        return true;
    }

    /**
     * 在一个文件底下创建文件的先决条件:parentNode是否存在,是否是目录类型
     */
    private boolean createNodePrecondition(String parentNodeName) {
        MavenRedisFileSystemNodeMetadata metadata = metadataAccessor.getMetadata(parentNodeName);
        if (metadata == null) {
            log.warn("can't create a node int parentNode:{},because parentNode:{} don't exist", parentNodeName, parentNodeName);
            return false;
        }
        MavenRedisFileSystemNodeMetadata.NodeType nodeType = metadata.getNodeType();
        if (!MavenRedisFileSystemNodeMetadata.NodeType.isFolderType(nodeType)) {
            log.warn("parentNode:{} is not a FOLDER or SOURCE_FOLDER,can't create a subNode", parentNodeName);
            return false;
        }
        return true;
    }

    public interface Accessor {
        /**
         * 生成redis当中的键值
         *
         * @param nodeName 生成key的参数
         * @return 在redis中的key
         */
        String key(String nodeName);

        /***
         * 判断一个节点在redis中是否存在
         * @param stringRedisTemplate redis连接模板
         * @param nodeName 节点名称
         */
        default boolean exist(StringRedisTemplate stringRedisTemplate, String nodeName) {
            boolean ret = false;
            Boolean aBoolean = stringRedisTemplate.hasKey(key(nodeName));
            if (aBoolean != null) {
                ret = aBoolean;
            }
            return ret;
        }
    }

    private static class CreationFailedException extends RuntimeException {
        public CreationFailedException() {
            super();
        }

        public CreationFailedException(String message) {
            super(message);
        }
    }

    public class RedisContentAccessor implements Accessor {

        @Override
        public String key(String nodeName) {
            nodeName = getAbsolutePath(nodeName);
            MavenRedisFileSystemNodeMetadata metadata = metadataAccessor.getMetadata(nodeName);
            if (metadata == null) {
                return null;
            }
            MavenRedisFileSystemNodeMetadata.ContentLink contentLink = metadata.getContentLink();
            if (TypeOfStorage.REDIS_SET == contentLink.getTypeOfStorage() ||
                    TypeOfStorage.REDIS_STRING == contentLink.getTypeOfStorage()) {
                return contentLink.getStoredLocation();
            }
            throw new UnsupportedOperationException("content is not in redis");
        }

        public boolean contentExist(String nodeName) {
            return exist(stringRedisTemplate, nodeName);
        }

        public void setAdd(String nodeName, String... content) {
            stringRedisTemplate.opsForSet().add(key(nodeName), content);
        }

        public void valueAdd(String nodeName, String content) {
            stringRedisTemplate.opsForValue().set(key(nodeName), content);
        }

        public Set<String> setGet(final String nodeName) {
            Set<String> members = stringRedisTemplate.opsForSet().members(key(nodeName));
            if (CollectionUtils.isEmpty(members)) {
                return Collections.emptySet();
            }
            return members.stream().map(s -> nodeName + FOLDER_SEGMENTATION + s).collect(Collectors.toSet());
        }

        public String valueGet(String nodeName) {
            String key = key(nodeName);
            if (StringUtils.isBlank(key)) {
                return null;
            }
            return stringRedisTemplate.opsForValue().get(key);
        }
    }

    public class MetadataAccessor implements Accessor {
        static final String KEY_PREFIX = "#matedata#";

        @Override
        public String key(String nodeName) {
            nodeName = getAbsolutePath(nodeName);
            return KEY_PREFIX + nodeName;
        }

        public boolean fileExist(String nodeName) {
            return exist(stringRedisTemplate, nodeName);
        }

        /**
         * 用于初始化一个节点的元信息
         */
        public void setNodeMetaDataInRedis(String nodeName, MavenRedisFileSystemNodeMetadata mavenRedisFileSystemNodeMetadata) throws JsonProcessingException {
            stringRedisTemplate.opsForValue().set(key(nodeName), new ObjectMapper().writeValueAsString(mavenRedisFileSystemNodeMetadata));

        }

        public MavenRedisFileSystemNodeMetadata getMetadata(String nodeName) {
            String key = key(nodeName);
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            MavenRedisFileSystemNodeMetadata ret = null;
            try {
                ret = new ObjectMapper().readValue(json, MavenRedisFileSystemNodeMetadata.class);
            } catch (IOException e) {
                log.error("{}", e);
            }
            return ret;
        }
    }


}
