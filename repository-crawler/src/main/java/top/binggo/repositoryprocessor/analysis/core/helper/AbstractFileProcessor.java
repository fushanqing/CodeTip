package top.binggo.repositoryprocessor.analysis.core.helper;

import codetip.commons.bean.CrawlerTaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import top.binggo.repositoryprocessor.analysis.Analyst;

import java.io.*;
import java.util.Arrays;

/**
 * @author binggo
 */
@Slf4j
public abstract class AbstractFileProcessor {
    protected String basePath;

    public static InputStream getInputStream(String filePath) throws IOException {
        log.info(filePath);
        return new FileInputStream(filePath);
    }

    public int processDir(String dirName, String basePath, Analyst analyst) throws IOException {
        log.info("process dir={}", dirName);
        this.basePath = basePath;
        File file = new File(dirName);
        int ret = 0;
        if (file.exists() && file.canRead() && file.isDirectory()) {
            ret = process(0, file, analyst);
        } else {
            log.warn("file.exists() && file.canRead() && file.isDirectory() == false");
        }
        return ret;
    }

    private int process(int oriCount, File file, Analyst analyst) throws IOException {
        if (file.isDirectory()) {
            int ret = 0;
            File[] listFiles = file.listFiles(getFileFilter());
            if (listFiles != null) {
                log.info("{} is a directory, list files{}", file.getName(), Arrays.toString(listFiles));
                for (File subFile : listFiles) {
                    ret += process(oriCount + ret, subFile, analyst);
                    if (analyst.getRedisTaskAccessor() != null) {
                        analyst.getRedisTaskAccessor().setTaskResultMsgInStatus(CrawlerTaskStatus.PROCESSING, analyst.getTaskId(), "process " + (ret + oriCount) + " files");
                    }
                }
            }
            return ret;
        } else {
            log.info("{} is file,process0 it", file.getName());
            return process0(file, analyst);
        }
    }

    protected FileFilter getFileFilter() {
        return AnalysisFileFilter.INSTANCE;
    }

    /**
     * 处理单个文件的函数
     *
     * @param file 要处理的文件
     * @param analyst
     * @return 文件如果被成功处理返回1, 否则返回0
     */
    protected abstract int process0(File file, Analyst analyst) throws IOException;

    public String getClassFullNameFromFile(File file) {
        String absolutePath = file.getAbsolutePath();
        return absolutePath.substring(basePath.length(), absolutePath.lastIndexOf(".")).replaceAll("\\\\", ".");
    }
}
