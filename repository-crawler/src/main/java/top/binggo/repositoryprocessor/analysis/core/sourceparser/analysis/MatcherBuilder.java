package top.binggo.repositoryprocessor.analysis.core.sourceparser.analysis;

import org.springframework.data.util.Pair;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.utils.RangeString;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.utils.StringUtils;

public interface MatcherBuilder<T> {
    Pair<T, String> build(RangeString rangeString, JavaDocs javaDocs, AnnotationUsingMembers annotationUsingMembers, JavaSourceNode fatherNode);

    default boolean match(RangeString rangeString, String equal) {
        return StringUtils.endWithFromIndex(rangeString.s, rangeString.to - equal.length(), equal);
    }
}
