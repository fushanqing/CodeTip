package top.binggo.repositoryprocessor.analysis.core.sourceparser.processor;

import codetip.commons.bean.MemberIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.binggo.repositoryprocessor.analysis.Analyst;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.analysis.JavaSourceNode;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.analysis.RootMembers;
import top.binggo.repositoryprocessor.analysis.sink.MemberIndexBulkRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author binggo
 */
@Slf4j
@Component
public class ProcessAndIndexProcessor extends LocalJavaSourceProcessor {


    final
    MemberIndexBulkRepository memberIndexBulkRepository;

    @Autowired
    public ProcessAndIndexProcessor(MemberIndexBulkRepository memberIndexBulkRepository) {
        this.memberIndexBulkRepository = memberIndexBulkRepository;
    }

    private static void preprocessData(String groupId, String artifactId, String version, List<MemberIndex> memberIndices, StringBuilder mavenIndexField, String source) {
        String sourceFileId = null;
        for (MemberIndex memberIndex : memberIndices) {
            if (memberIndex == null) {
                continue;
            }
            memberIndex.setArtifactId(artifactId);
            memberIndex.setGroupId(groupId);
            memberIndex.setVersion(version);
            memberIndex.setIndexId(groupId + "/" + artifactId + "/" + version + "/" + memberIndex.getId());
            if (JavaSourceNode.Type.SOURCE_FILE.toString().equals(memberIndex.getType())) {
                sourceFileId = memberIndex.getIndexId();
                memberIndex.setSourceFileId(sourceFileId);
                memberIndex.setSource(source);
                mavenIndexField.append(memberIndex.getIndexField());
            } else {
                memberIndex.setSourceFileId(sourceFileId);
            }
        }
    }

    public MemberIndexBulkRepository getMemberIndexBulkRepository() {
        return memberIndexBulkRepository;
    }

    @Override
    public void processAfterSourceCodeAnalysis(RootMembers rootMembers, File file, Analyst analyst, String s) throws IOException {
        log.info("index the result of file={}", file.getAbsoluteFile());
        List<MemberIndex> memberIndices = RootMembers.toMemberIndices(rootMembers);
        preprocessData(analyst.groupId, analyst.artifactId, analyst.version, memberIndices, analyst.mavenIndexField, s);
        memberIndexBulkRepository.bulkIndex(memberIndices);
    }


}
