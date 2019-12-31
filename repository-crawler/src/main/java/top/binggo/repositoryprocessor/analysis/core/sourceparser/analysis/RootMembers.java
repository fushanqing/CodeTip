package top.binggo.repositoryprocessor.analysis.core.sourceparser.analysis;

import codetip.commons.bean.MemberIndex;
import com.google.common.collect.ImmutableSet;
import lombok.NoArgsConstructor;
import org.springframework.data.util.Pair;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.constant.JavaKeyWords;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.utils.RangeString;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.utils.StringUtils;

import java.util.*;

/**
 * @author binggo
 */
public class RootMembers implements Creator {
    static final Set<JavaSourceNode.Type> TYPE_SET = ImmutableSet.of(
            JavaSourceNode.Type.SOURCE_FILE,
            JavaSourceNode.Type.CLASS,
            JavaSourceNode.Type.INTERFACE,
            JavaSourceNode.Type.ENUM,
            JavaSourceNode.Type.ANNOTATION
    );

    private String fileName = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16) + JavaKeyWords.SOURCE_SUFFIX;

    List<RootMember> rootMemberList = new ArrayList<>();

    public RootMembers() {
    }

    public RootMembers(String fileName) {
        this.fileName = fileName;
    }

    public static List<MemberIndex> toMemberIndices(RootMembers rootMembers) {
        if (!rootMembers.rootMemberList.isEmpty()) {
            RootMember rootMember = rootMembers.rootMemberList.get(0);
            return toMemberIndices0(rootMember);
        } else {
            return Collections.emptyList();
        }
    }

    private static List<MemberIndex> toMemberIndices0(JavaSourceNode fatherNode) {
        List<MemberIndex> memberIndices = new ArrayList<>();
        MemberIndex.MemberIndexBuilder memberIndexBuilder = MemberIndex.builder().
                accessLevel(fatherNode.getAccessLevel())
                .haveEnoughDescriptiveInfo(true)
                .id(fatherNode.getId())
                .description(fatherNode.getDescription())
                .indexField(fatherNode.getIndexField())
                .symbol(fatherNode.getSymbol())
                .indexInSource(fatherNode.getLocationInFile())
                .type(fatherNode.getType().toString())
                .source(fatherNode.toString());
        if (TYPE_SET.contains(fatherNode.getType())) {
            memberIndexBuilder.className(fatherNode.getId());
        } else {
            JavaSourceNode ff = fatherNode.getFather();
            memberIndexBuilder.className(ff == null ? "" : ff.getId());
        }
        memberIndices.add(memberIndexBuilder.build());
        for (JavaSourceNode child : fatherNode.getChildren()) {
            memberIndices.addAll(toMemberIndices0(child));
        }
        return memberIndices;
    }


    @Override

    public Pair<Integer, String> create(RangeString sourceCode, JavaDocs javaDocs, AnnotationUsingMembers annotationUsingMembers, JavaSourceNode fatherNode) {
        Pair<Integer, String> defualt = Creator.defualt(sourceCode, javaDocs, annotationUsingMembers, fatherNode, RootMember.builder(), this.rootMemberList);
        for (RootMember rootMember : rootMemberList) {
            rootMember.fileName = this.fileName;
        }
        return defualt;
    }

    @Override
    public String toString() {
        return StringUtils.toString(StringUtils.list2String(this.rootMemberList));
    }

    @NoArgsConstructor
    static class RootMember implements SourceCodeRange, JavaSourceNode {
        String fileName;
        JavaSourceNode fatherNode;
        RangeString rangeString;
        ClassMembers classMembers = new ClassMembers();
        EnumMembers enumMembers = new EnumMembers();
        AnnotationMembers annotationMembers = new AnnotationMembers();
        FieldMembers fieldMembers = new FieldMembers();
        JavaDocs javaDocs = new JavaDocs();
        AnnotationUsingMembers annotationUsingMembers = new AnnotationUsingMembers();
        Creator[] creators = {this.classMembers, this.enumMembers, this.annotationMembers, this.fieldMembers, this.javaDocs, this.annotationUsingMembers};

        public static RootMembers.RootMember.Builder builder() {
            return new RootMembers.RootMember().new Builder();
        }

        @Override
        public String toString() {
            return StringUtils.toString(this.fieldMembers, this.classMembers, this.enumMembers, this.annotationMembers);
        }

        @Override
        public RangeString getRangeString() {
            return this.rangeString;
        }

        @Override
        public JavaSourceNode getFather() {
            return this.fatherNode;
        }

        @Override
        public String getAccessLevelString() {
            return JavaKeyWords.PUBLIC;
        }

        @Override
        public String splitString() {
            return "";
        }

        @Override
        public String getSymbol() {
            Optional<FieldMembers.FieldMember> first = this.fieldMembers.fieldMemberList.stream().filter(fieldMember -> StringUtils.prettyString(fieldMember.rangeString).startsWith(JavaKeyWords.PACKAGE)).findFirst();
            String ret = "";
            if (first.isPresent()) {
                String s = StringUtils.prettyString(first.get().rangeString).split(" ", 2)[1];
                ret = s.substring(0, s.length() - 1);
            }
            return ret + "." + fileName;
        }


        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public String getJavaDoc() {
            return "";
        }


        @Override
        public int getLocationInFile() {
            return 0;
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<JavaSourceNode> getChildren() {
            return JavaSourceNode.mergeList(this.classMembers.classMemberList, this.enumMembers.enumMemberList, this.annotationMembers.annotationMemberList);
        }

        @Override
        public Type getType() {
            return Type.SOURCE_FILE;
        }

        @Override
        public int getDefaultAccessLevelForChild() {
            return FRIENDLY_LEVEL;
        }

        class Builder implements MatcherBuilder<RootMembers.RootMember> {

            /**
             * rangeString = '其他字符{'
             */
            @Override
            public Pair<RootMembers.RootMember, String> build(RangeString rangeString, JavaDocs javaDocs, AnnotationUsingMembers annotationUsingMembers, JavaSourceNode fatherNode) {
                Pair<Integer, String> classOrInterface = Creator.createClassOrInterface(rangeString, creators, RootMember.this.javaDocs, RootMember.this.annotationUsingMembers, RootMember.this, null);
                RootMember.this.rangeString = RangeString.of(rangeString.from, classOrInterface.getFirst(), rangeString.s);
                RootMember.this.fatherNode = null;
                return Pair.of(RootMember.this, classOrInterface.getSecond());
            }
        }


    }
}
