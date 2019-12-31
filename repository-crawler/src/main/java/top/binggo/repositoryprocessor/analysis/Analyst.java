package top.binggo.repositoryprocessor.analysis;

import codetip.commons.bean.CrawlerTaskStatus;
import codetip.commons.bean.MemberIndex;
import codetip.commons.bean.Task;
import codetip.commons.crawl.RedisTaskAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import top.binggo.codetip.filesystem.core.MavenRedisFileSystemAccessor;
import top.binggo.codetip.filesystem.core.MavenRedisFileSystemNodeMetadata;
import top.binggo.repositoryprocessor.analysis.core.helper.FolderHelper;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.analysis.JavaSourceNode;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.processor.ProcessAndIndexProcessor;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

/**
 * @author binggo
 */
@Slf4j
public class Analyst implements Runnable {
    private static final String BASE_PATH =
//            "D:\\codeTipSourceJar";
            FolderHelper.getBaseDir();
    public final StringBuilder mavenIndexField = new StringBuilder();
    public String groupId;
    public String artifactId;
    public String version;
    JavaSourceDownloader javaSourceDownloader = new AliMavenRepoCentralJavaSourceDownloader();
    String targetDir = BASE_PATH + FolderHelper.getSpliter() + UUID.randomUUID().toString().substring(0, 16);
    String jarFileName = targetDir + ".jar";
    ProcessAndIndexProcessor processAndIndexProcessor;
    private RedisTaskAccessor redisTaskAccessor;
    private String taskId;
    private AnalysisApplication analysisApplication;
    private String subTaskId;
    private MavenRedisFileSystemAccessor mavenRedisFileSystemAccessor;

    public Analyst(String subTaskId, AnalysisApplication analysisApplication, String groupId, String artifactId, String version, ProcessAndIndexProcessor processAndIndexProcessor, RedisTaskAccessor redisTaskAccessor, String taskId, MavenRedisFileSystemAccessor mavenRedisFileSystemAccessor) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.processAndIndexProcessor = processAndIndexProcessor;
        this.redisTaskAccessor = redisTaskAccessor;
        this.taskId = taskId;
        this.analysisApplication = analysisApplication;
        this.subTaskId = subTaskId;
        this.mavenRedisFileSystemAccessor = mavenRedisFileSystemAccessor;
    }

    public String getTaskId() {
        return taskId;
    }


    public RedisTaskAccessor getRedisTaskAccessor() {
        return redisTaskAccessor;
    }

    @Override
    public void run() {
        //现在文件到文件系统
        File sourceFiles = null;
        String resultString = "任务执行成功";
        try {
            sourceFiles = getSourceFiles();
        } catch (Exception e) {
            log.error("{}",e);
            resultString = "源文件下载出错";
        }
        if (sourceFiles == null) {
            log.warn("can not get source file for groupId={} artifactId={} version={}",groupId,artifactId,version);
            resultString = "源文件下载出错";

        }else {
            try {
                //下载pom文件
                InputStream download = javaSourceDownloader.download(groupId, artifactId, version, ".pom");
                updateNodeStatus(1, 0, 0);
                String string = download == null ? "" : IOUtils.toString(download);
                //分析程序进行分析
                int i = processAndIndexProcessor.processDir(sourceFiles.getAbsolutePath(), sourceFiles.getAbsolutePath(), this);
                log.info("process {} files", i);
                MemberIndex.MemberIndexBuilder builder = MemberIndex.builder();
                String id = groupId + "/" + artifactId + "/" + version;
                MemberIndex build = builder.accessLevel(JavaSourceNode.PUBLIC_LEVEL)
                        .groupId(groupId)
                        .artifactId(artifactId)
                        .version(version).indexField(mavenIndexField.toString())
                        .description(id)
                        .symbol(artifactId)
                        .id(id)
                        .indexId(id)
                        .source(string)
                        .type(JavaSourceNode.Type.PROJECT.toString())
                        .build();
                processAndIndexProcessor.getMemberIndexBulkRepository().bulkIndex(Lists.newArrayList(build));
                processAndIndexProcessor.getMemberIndexBulkRepository().flush();
                updateNodeStatus(1, 1, 0);
                boolean deleteFlag = true;
                deleteFlag = deleteFlag && (sourceFiles.delete());
                deleteFlag = deleteFlag && (new File(sourceFiles.getAbsoluteFile() + ".jar").delete());
                if (!deleteFlag) {
                    log.error("delete error");
                }
            } catch (Exception e) {
                log.error("{}", e);
                resultString = "源文件解析出错";
            }
        }
        analysisApplication.runningAnalyst.remove(subTaskId);
        log.info("finish subTask={}", subTaskId);
        boolean finishTaskFlag = true;
        for (Map.Entry<String, Analyst> stringAnalystEntry : analysisApplication.runningAnalyst.entrySet()) {
            if (stringAnalystEntry.getKey().startsWith(taskId)) {
                finishTaskFlag = false;
                break;
            }
        }
        Task task = redisTaskAccessor.getATaskInStatus(CrawlerTaskStatus.PROCESSING, taskId);
        String resultMsg = task==null?"":task.getResultMsg();
        if (resultMsg != null) {
            resultMsg+=groupId+"/"+artifactId+"/"+version+"/"+resultString;
        }
        redisTaskAccessor.setTaskResultMsgInStatus(CrawlerTaskStatus.PROCESSING, taskId, resultMsg);
        if (finishTaskFlag) {
            redisTaskAccessor.finishATask(taskId);
            log.info("finish task={}", taskId);
        }
    }

    private void updateNodeStatus(int status1, int status2, int status3) throws JsonProcessingException {
        if (mavenRedisFileSystemAccessor != null) {
            String metadataFileName = "#root/" + groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + "maven-metadata-public.xml";
            MavenRedisFileSystemNodeMetadata metadata = mavenRedisFileSystemAccessor.getMetadataAccessor().getMetadata(metadataFileName);
            metadata.setFolderNodeStatus(new MavenRedisFileSystemNodeMetadata.FolderNodeStatus(status1, status2, status3));
            mavenRedisFileSystemAccessor.getMetadataAccessor().setNodeMetaDataInRedis(metadataFileName, metadata);
        }
    }

    private File getSourceFiles() {
        File file = null;
        try {
            InputStream download = javaSourceDownloader.download(groupId, artifactId, version, "-sources.jar");
            if (download == null) {
                log.warn("can not download file source for groupId={} artifactId={} version={}",groupId,artifactId,version);
                return null;
            }
            File jarFile = inputStream2JarFile(jarFileName, download);
            if (jarFile == null) {
                log.error("can not create file={}", jarFileName);
                return null;
            }
            file = unJarFile(jarFile, targetDir);
            if (file == null) {
                log.error("unJar fail ,content={}", targetDir);
            }
        } catch (IOException e) {
            log.error("{}", e);
        }
        return file;
    }

    private File unJarFile(File file, String outputPath) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        JarInputStream jarInputStream = new JarInputStream(inputStream);
        JarFile jarFile = new JarFile(file);
        File outputFile = new File(outputPath);
        if (!outputFile.exists() && !outputFile.mkdir()) {
            return null;
        }
        ZipEntry nextEntry;
        int bufferSize = 2048;
        byte[] bytes = new byte[bufferSize];
        while ((nextEntry = jarInputStream.getNextEntry()) != null) {
            String fileName = nextEntry.getName();
            File entryFile = new File(outputFile.getAbsoluteFile() + FolderHelper.getSpliter() + fileName);
            if (!createParent(entryFile)) {
                return null;
            }
            if (nextEntry.isDirectory()) {
                if (!entryFile.mkdir()) {
                    return null;
                }
                continue;
            }
            int len;
            InputStream entryIS = jarFile.getInputStream(nextEntry);
            FileOutputStream entryOS = new FileOutputStream(entryFile);
            while ((len = entryIS.read(bytes)) != -1) {
                entryOS.write(bytes, 0, len);
            }
        }
        return outputFile;
    }

    private File inputStream2JarFile(String fileName, InputStream inputStream) throws IOException {
        File file = new File(fileName);
        if (!createParent(file)) {
            return null;
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        int bufferSize = 2048;
        byte[] bytes = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(bytes)) != -1) {
            fileOutputStream.write(bytes, 0, len);
        }
        inputStream.close();
        fileOutputStream.close();
        return file;
    }

    private boolean createParent(File file) {
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            if (!parentFile.mkdir()) {
                log.error("can not create dir:{}", parentFile.getAbsoluteFile());
                return false;
            }
        }
        return true;
    }
}
