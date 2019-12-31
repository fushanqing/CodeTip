package top.binggo.repositoryprocessor.analysis.core.sourceparser.analysis;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import top.binggo.repositoryprocessor.analysis.core.helper.CamelNameProcessor;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.constant.JavaKeyWords;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author binggo
 */
public interface JavaSourceNode extends SourceCodeRange {
    /**
     * 节点的访问权限
     */
    int PUBLIC_LEVEL = 3;
    int FRIENDLY_LEVEL = 2;
    int PROTECTED_LEVEL = 1;
    int PRIVATE_LEVEL = 0;

    Map<String, Integer> MAP = ImmutableMap.of(JavaKeyWords.PUBLIC, PUBLIC_LEVEL,
            JavaKeyWords.PROTECTED, PROTECTED_LEVEL,
            JavaKeyWords.PRIVATE, PRIVATE_LEVEL);

    Set<String> STRING_IMMUTABLE_SET = ImmutableSet.of(
            JavaKeyWords.CLASS, JavaKeyWords.INTERFACE,
            JavaKeyWords.ENUM, JavaKeyWords.ANNOTATION,
            JavaKeyWords.PUBLIC, JavaKeyWords.PROTECTED,
            JavaKeyWords.PRIVATE, JavaKeyWords.STATIC,
            JavaKeyWords.FINAL, JavaKeyWords.ABSTRACT
    );

    static int getAccessLevelFromRoot(JavaSourceNode javaSourceNode) {
        JavaSourceNode father = javaSourceNode;
        int ret = javaSourceNode.getAccessLevel();
        while ((father = father.getFather()) != null) {
            ret = Math.min(ret, father.getAccessLevel());
            if (ret == PRIVATE_LEVEL) {
                return 0;
            }
        }
        return ret;
    }

    static List<JavaSourceNode> mergeList(List<? extends JavaSourceNode>... lists) {
        ArrayList<JavaSourceNode> ret = new ArrayList<>(64);
        for (List<? extends JavaSourceNode> list : lists) {
            ret.addAll(list);
        }
        return ret;
    }


    static String getSymbolForClass(String head) {
        String[] strings = STRING_IMMUTABLE_SET.toArray(new String[0]);
        for (int i = 0; i < strings.length; i++) {
            strings[i] = strings[i] + " ";
        }
        String string = StringUtils.prettyString(head, 0, head.length());
        for (int i = 0; i < string.length(); ) {
            int i1 = StringUtils.startWithAnyFromIndex(string, i, strings);
            if (i1 != -1) {
                i += strings[i1].length();
            } else {
                int j;
                for (j = i; j < string.length(); j++) {
                    if (!Character.isJavaIdentifierPart(string.charAt(j))) {
                        break;
                    }
                }
                return string.substring(i, j);
            }
        }
        return string;
    }

    static String javaDocList2String(List<JavaDocs.JavaDoc> javaDocOverClass) {
        StringBuilder ret = new StringBuilder();

        for (JavaDocs.JavaDoc docOverClass : javaDocOverClass) {
            ret.append(docOverClass.rangeString.subString()).append(JavaKeyWords.ENTRY);
        }
        return ret.toString();

    }

    /**
     * 获得该节点的父亲节点
     */
    JavaSourceNode getFather();

    /**
     * 如果直接点没有访问修饰符定义，那么他的默认访问权限
     * 对于class是friendly
     */
    default int getDefaultAccessLevelForChild() {
        return PUBLIC_LEVEL;
    }

    /**
     * 获得子接访问权限，即不考虑父节点的访问权限
     */
    default int getAccessLevel() {
        String string = getAccessLevelString();
        for (Map.Entry<String, Integer> entry : MAP.entrySet()) {
            if (string.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        JavaSourceNode father = getFather();
        return father == null ? PUBLIC_LEVEL : father.getDefaultAccessLevelForChild();

    }

    /**
     * 带有访问权限修饰符号的String
     */
    String getAccessLevelString();

    /**
     * 获得一个在当前源文件中的唯一id
     * */
    default String getId() {
        JavaSourceNode father = getFather();
        String s = father == null ? "" : father.getId();
        s += splitString() + getSymbol();
        return s;
    }

    /**
     * 用来分隔的字符串
     * */
    String splitString();

    /**
     * 获得标识符号
     * */
    String getSymbol();

    /**
     * 获得描述信息
     * */
    String getDescription();

    /**
     * 获得javaDoc的内容
     * */
    String getJavaDoc();

    /**
     * 描述符号的权重
     * */
    default int getSymbolWeights() {
        return 5;
    }

    /**
     * 获得索引的内容
     * */
    default String getIndexField() {
        StringBuilder ret = new StringBuilder();
        String symbol = getSymbol();
        String turn = CamelNameProcessor.turn(symbol);
        for (int i = 0; i < getSymbolWeights(); i++) {
            ret.append(turn).append(JavaKeyWords.ENTRY);
        }
        //todo prettyJavaDoc
        ret.append(getJavaDoc());
        List<JavaSourceNode> children = getChildren();
        for (JavaSourceNode child : children) {
            ret.append(child.getIndexField()).append(JavaKeyWords.ENTRY);
        }
        return ret.toString();
    }

    /**
     * 获得在源文件中的位置
     * */
    int getLocationInFile();

    /**
     * 获得所有孩子节点
     * */
    List<JavaSourceNode> getChildren();

    /**
     * 获得类型
     */
    Type getType();


    enum Type {
        /**
         * .java源文件
         */
        SOURCE_FILE,
        CLASS,
        INTERFACE,
        ENUM,
        ANNOTATION,
        FIELD,
        METHOD,
        JAVADOC,
        ANNOTATION_USING,
        PROJECT
    }
}
