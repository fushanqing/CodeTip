package top.binggo.repositoryprocessor.analysis.core.sourceparser.analysis;

import top.binggo.repositoryprocessor.analysis.core.sourceparser.utils.RangeString;

/**
 * @author binggo
 */
public interface SourceCodeRange {

    /**
     * 获得所表示的代码区间
     *
     * @return 获得所表示的代码区间
     */
    RangeString getRangeString();
}
