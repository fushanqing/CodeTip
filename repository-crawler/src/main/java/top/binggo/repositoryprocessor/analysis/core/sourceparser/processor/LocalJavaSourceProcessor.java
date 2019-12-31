package top.binggo.repositoryprocessor.analysis.core.sourceparser.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import top.binggo.repositoryprocessor.analysis.Analyst;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.analysis.RootMembers;
import top.binggo.repositoryprocessor.analysis.core.helper.AbstractFileProcessor;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.utils.RangeString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 用于对单个.java源文件进行处理
 * @author binggo
 */
@Slf4j
public abstract class LocalJavaSourceProcessor extends AbstractFileProcessor {

    @Override
    protected int process0(File file, Analyst analyst) throws IOException {
        InputStream inputStream = getInputStream(file.getAbsolutePath());
        String s = IOUtils.toString(inputStream);
        RootMembers rootMembers = new RootMembers(file.getName());
        try {
            rootMembers.create(RangeString.of(0, s.length(), s), null, null, null);
        } catch (Exception e) {
            log.error("process file={} meet exception={}", file.getAbsolutePath(), e);
            return 0;
        }
        processAfterSourceCodeAnalysis(rootMembers, file, analyst, s);
        return 1;
    }

    public abstract void processAfterSourceCodeAnalysis(RootMembers rootMembers, File file, Analyst analyst, String s) throws IOException;


}
