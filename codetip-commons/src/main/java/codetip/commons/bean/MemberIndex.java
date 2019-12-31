package codetip.commons.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.search.SearchHit;

import java.util.Map;

/**
 * @author binggo
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberIndex {

    /**
     * 在某个项目中的唯一id
     */
    private String id;
    /**
     * 在ES中的唯一id
     */
    private String indexId;

    /**
     * 存储的内容的类型 {@link top.binggo.repositoryprocessor.analysis.core.sourceparser.analysis.ChildNode}
     */
    private String type;

    /**
     * maven坐标
     */
    private String groupId;

    private String artifactId;

    private String version;

    /**
     * 类的名称，没有为空
     * */
    private String className;

    /**
     * 表示符号，用作暂时
     * */
    private String symbol;

    /**
     * 时候包含足够的描述信息
     */
    private Boolean haveEnoughDescriptiveInfo;

    /**
     * 描述信息域，用作展示
     */
    private String description;

    /**
     * 索引域，用作全文检索
     * */
    private String indexField;

    /**
     * 访问权限 {@link top.binggo.repositoryprocessor.analysis.core.sourceparser.analysis.ChildNode}
     * */
    private Integer accessLevel;

    /**
     * 内容信息，用作展示
     */
    private String source;

    /**
     * 是否是被推荐的版本
     * */
    private Boolean recommendVersion;

    /**
     * 源文件索引的Id
     */
    private String sourceFileId;

    private Integer indexInSource;

    public static MemberIndex fromSearchHit(SearchHit searchHit) {
        Map<String, Object> fields = searchHit.getSourceAsMap();
        return MemberIndex.builder()
                .groupId(getValueFromDocument(fields, "groupId"))
                .artifactId(getValueFromDocument(fields, "artifactId"))
                .version(getValueFromDocument(fields, "version"))
                .type(getValueFromDocument(fields, "type"))
                .symbol(getValueFromDocument(fields, "symbol"))
                .indexId(getValueFromDocument(fields, "indexId"))
                .id(getValueFromDocument(fields, "id"))
                .description(getValueFromDocument(fields, "description"))
                .source(getValueFromDocument(fields, "source"))
                .sourceFileId(getValueFromDocument(fields, "sourceFileId"))
                .indexField(getValueFromDocument(fields, "indexField"))
                .accessLevel(getInteger(getValueFromDocument(fields, "accessLevel")))
                .className(getValueFromDocument(fields, "className"))
                .haveEnoughDescriptiveInfo(getBoolean(getValueFromDocument(fields, "haveEnoughDescriptiveInfo")))
                .recommendVersion(getBoolean(getValueFromDocument(fields, "recommendVersion")))
                .indexInSource(getInteger(getValueFromDocument(fields, "indexInSource"))).build();
    }


    private static Integer getInteger(String s) {
        if (s == null) {
            return null;
        }
        return Integer.parseInt(s);
    }

    private static Boolean getBoolean(String s) {
        if (s == null) {
            return null;
        }
        return Boolean.parseBoolean(s);
    }

    private static String getValueFromDocument(Map<String, Object> field, String fieldName) {
        Object o = field.get(fieldName);
        if (o == null) {
            return null;
        }
        return o.toString();
    }
}
