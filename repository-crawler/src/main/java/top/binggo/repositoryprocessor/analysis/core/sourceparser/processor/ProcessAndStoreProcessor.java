package top.binggo.repositoryprocessor.analysis.core.sourceparser.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import top.binggo.repositoryprocessor.analysis.Analyst;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.analysis.RootMembers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author binggo
 */
@Slf4j
public class ProcessAndStoreProcessor extends LocalJavaSourceProcessor {
    private String otherDir;

    public ProcessAndStoreProcessor(String otherDir) {
        this.otherDir = otherDir;
    }

    @Override
    public void processAfterSourceCodeAnalysis(RootMembers rootMembers, File file, Analyst analyst, String s) throws IOException {
        String path = otherDir + file.getAbsolutePath().substring(basePath.length());
        File file1 = new File(path);
        boolean newFile = true;
        createFileP(path);
        if (!file1.exists()) {
            newFile = file1.createNewFile();
        }
        if (newFile) {
            IOUtils.write(rootMembers.toString(), new FileOutputStream(file1));
        }
    }

    private void createFileP(String path) {
        String[] split = path.split("\\\\");
        StringBuilder prefix = new StringBuilder(split[0]);
        for (int i = 1; i < split.length - 1; i++) {
            prefix.append("\\").append(split[i]);
            File file = new File(prefix.toString());
            if (!file.exists()) {
                boolean mkdir = file.mkdir();
                if (!mkdir) {
                    log.error("can't create dir={}", prefix.toString());
                }
            }
        }
    }
}
