package top.binggo.repositoryprocessor.crawler.pipeline;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author binggo
 */
public class LocalFileStorageStrategy implements StorageStrategy {
    private boolean enable = true;
    private File targetDir;

    private LocalFileStorageStrategy(File targetDir) {
        this.targetDir = targetDir;
    }

    public static LocalFileStorageStrategy of(String targetDir) {
        File file = new File(targetDir);
        Preconditions.checkState(file.isDirectory(), "%s is not directory", file.getAbsolutePath());
        return new LocalFileStorageStrategy(file);
    }

    @Override
    public boolean enable() {
        return enable;
    }

    @Override
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public String getTargetInfo() {
        return targetDir.getAbsolutePath();
    }

    @Override
    public void sink(String fileName, String content) throws IOException {
        String targetFileName = targetDir.getAbsolutePath() + "\\" + fileName;
        File targetFile = new File(targetFileName);
        FileUtils.writeStringToFile(targetFile, content);
    }
}
