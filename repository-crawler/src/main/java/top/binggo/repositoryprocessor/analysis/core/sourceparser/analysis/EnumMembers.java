package top.binggo.repositoryprocessor.analysis.core.sourceparser.analysis;

import org.springframework.data.util.Pair;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.constant.JavaKeyWords;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.utils.RangeString;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author binggo
 */
public class EnumMembers implements Creator {
    List<EnumMember> enumMemberList = new ArrayList<>();

    @Override
    public List<? extends JavaSourceNode> getChildNodeMember() {
        return enumMemberList;
    }

    @Override
    public Pair<Integer, String> create(RangeString sourceCode, JavaDocs javaDocs, AnnotationUsingMembers annotationUsingMembers, JavaSourceNode fatherNode) {
        return Creator.defualt(sourceCode, javaDocs, annotationUsingMembers, fatherNode, EnumMembers.EnumMember.builder(), this.enumMemberList);
    }

    @Override
    public String toString() {
        return StringUtils.toString(StringUtils.list2String(this.enumMemberList));
    }

    public static class EnumMember implements SourceCodeRange, JavaSourceNode {
        String head = "";
        JavaSourceNode fatherNode;
        RangeString rangeString;
        EnumFieldMembers enumFieldMembers = new EnumFieldMembers();
        MethodMembers methodMembers = new MethodMembers();
        FieldMembers fieldMembers = new FieldMembers();
        BlockMembers blockMembers = new BlockMembers();
        JavaDocs javaDocs = new JavaDocs();
        AnnotationUsingMembers annotationUsingMembers = new AnnotationUsingMembers();
        AnnotationMembers annotationMembers = new AnnotationMembers();

        ClassMembers classMembers = new ClassMembers();
        EnumMembers enumMembers = new EnumMembers();
        CommaAndSemicolonMembers commaAndSemicolonMembers = new CommaAndSemicolonMembers();

        List<JavaDocs.JavaDoc> javaDocOverField;
        List<AnnotationUsingMembers.AnnotationUsingMember> annotationOverField;

        Creator[] creators = {
                this.javaDocs,
                this.annotationUsingMembers,
                this.commaAndSemicolonMembers,
                this.methodMembers,
                this.fieldMembers,
                this.blockMembers,
                this.classMembers,
                this.enumMembers,
                this.annotationMembers,
                //要是最后一个
                this.enumFieldMembers,
        };

        public static EnumMembers.EnumMember.Builder builder() {
            return new EnumMembers.EnumMember().new Builder();
        }

        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder(StringUtils.toString(StringUtils.list2String(javaDocOverField), StringUtils.list2String(annotationOverField), head, "{"));
            Creator[] creators = {this.enumFieldMembers, this.fieldMembers, this.blockMembers, this.methodMembers, this.classMembers, this.enumMembers, this.annotationMembers, this.commaAndSemicolonMembers};
            int[] points = new int[creators.length];
            while (true) {
                int minIndex = -1;
                int targetId = -1;
                boolean breakFlag = true;
                for (int i = 0; i < creators.length; i++) {
                    Creator creator = creators[i];
                    List<? extends JavaSourceNode> childNodeMember = creator.getChildNodeMember();
                    if (childNodeMember.size() > points[i]) {
                        JavaSourceNode javaSourceNode = childNodeMember.get(points[i]);
                        RangeString rangeString = javaSourceNode.getRangeString();
                        if (targetId == -1 || rangeString.from < minIndex) {
                            minIndex = rangeString.from;
                            targetId = i;
                            breakFlag = false;
                        }
                    }

                }
                if (breakFlag) {
                    break;
                } else {
                    JavaSourceNode obj = creators[targetId].getChildNodeMember().get(points[targetId]);
                    ret.append(obj);
                    points[targetId]++;
                }
            }
            return ret.append("}").toString();
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
        public int getDefaultAccessLevelForChild() {
            return FRIENDLY_LEVEL;
        }

        @Override
        public String getAccessLevelString() {
            return head;
        }

        @Override
        public String splitString() {
            return JavaKeyWords.DOT;
        }

        @Override
        public String getSymbol() {
            return JavaSourceNode.getSymbolForClass(head);
        }

        @Override
        public String getDescription() {
            return head;
        }

        @Override
        public String getJavaDoc() {
            return JavaSourceNode.javaDocList2String(this.javaDocOverField);
        }

        @Override
        public int getLocationInFile() {
            return rangeString.from;
        }


        @Override
        public List<JavaSourceNode> getChildren() {
            return JavaSourceNode.mergeList(this.enumFieldMembers.enumFieldMemberList, this.classMembers.classMemberList, this.enumMembers.enumMemberList, this.annotationMembers.annotationMemberList);
        }

        @Override
        public Type getType() {
            return Type.ENUM;
        }


        class Builder implements MatcherBuilder<EnumMembers.EnumMember> {

            /**
             * rangeString = '其他字符{'
             */
            @Override
            public Pair<EnumMember, String> build(RangeString rangeString, JavaDocs javaDocs, AnnotationUsingMembers annotationUsingMembers, JavaSourceNode fatherNode) {
                String s = StringUtils.prettyString(rangeString);
                RangeString of = RangeString.of(0, s.length(), s);
                if (s.endsWith(JavaKeyWords.LEFT_CURLY_BRACES) && StringUtils.checkClassType(of, JavaKeyWords.ENUM)) {
                    head = s.substring(0, s.length() - JavaKeyWords.LEFT_CURLY_BRACES.length());
                    javaDocOverField = javaDocs.getLastJavaDocList();
                    annotationOverField = annotationUsingMembers.getLastAnnotationUsingMemberList();
                    Pair<Integer, String> classOrInterface = Creator.createClassOrInterface(rangeString.left(), creators, EnumMember.this.javaDocs, EnumMember.this.annotationUsingMembers, EnumMember.this, EnumMember.this.enumFieldMembers);
                    EnumMember.this.rangeString = RangeString.of(rangeString.from, classOrInterface.getFirst(), rangeString.s);
                    return Pair.of(EnumMember.this, classOrInterface.getSecond());

                }
                return null;
            }
        }
    }

    static class CommaAndSemicolonMembers implements Creator {

        List<CommaAndSemicolonMember> commaAndSemicolonMembers = new ArrayList<>();

        @Override
        public List<? extends JavaSourceNode> getChildNodeMember() {
            return commaAndSemicolonMembers;
        }

        @Override
        public String toString() {
            return StringUtils.list2String(commaAndSemicolonMembers);
        }

        @Override
        public Pair<Integer, String> create(RangeString sourceCode, JavaDocs javaDocs, AnnotationUsingMembers annotationUsingMembers, JavaSourceNode fatherNode) {
            String string = StringUtils.prettyString(sourceCode);
            if (string.equals(JavaKeyWords.COMMA) || string.equals(JavaKeyWords.SEMICOLON)) {
                CommaAndSemicolonMember commaAndSemicolonMember = new CommaAndSemicolonMember(RangeString.of(sourceCode.from, sourceCode.to, sourceCode.s));
                this.commaAndSemicolonMembers.add(commaAndSemicolonMember);
                return Pair.of(sourceCode.to, sourceCode.s);
            }
            return Pair.of(-1, sourceCode.s);
        }

        static class CommaAndSemicolonMember implements JavaSourceNode {
            private RangeString rangeString;

            public CommaAndSemicolonMember(RangeString rangeString) {
                this.rangeString = rangeString;
            }

            @Override
            public String toString() {
                return rangeString.subString();
            }

            @Override
            public JavaSourceNode getFather() {
                return null;
            }

            @Override
            public String getAccessLevelString() {
                return null;
            }

            @Override
            public String splitString() {
                return null;
            }

            @Override
            public String getSymbol() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public String getJavaDoc() {
                return null;
            }

            @Override
            public int getLocationInFile() {
                return 0;
            }

            @Override
            public List<JavaSourceNode> getChildren() {
                return null;
            }

            @Override
            public Type getType() {
                return null;
            }

            @Override
            public RangeString getRangeString() {
                return this.rangeString;
            }
        }
    }
}
