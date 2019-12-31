package top.binggo.repositoryprocessor.analysis.core.sourceparser.analysis;

import lombok.NoArgsConstructor;
import org.springframework.data.util.Pair;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.constant.JavaKeyWords;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.utils.RangeString;
import top.binggo.repositoryprocessor.analysis.core.sourceparser.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author binggo
 */
public class ClassMembers implements Creator {
    List<ClassMember> classMemberList = new ArrayList<>();

    @Override
    public List<? extends JavaSourceNode> getChildNodeMember() {
        return classMemberList;
    }

    @Override
    public Pair<Integer, String> create(RangeString sourceCode, JavaDocs javaDocs, AnnotationUsingMembers annotationUsingMembers, JavaSourceNode fatherNode) {
        return Creator.defualt(sourceCode, javaDocs, annotationUsingMembers, fatherNode, ClassMember.builder(), this.classMemberList);
    }

    @Override
    public String toString() {
        return StringUtils.toString(StringUtils.list2String(this.classMemberList));
    }

    @NoArgsConstructor
    static class ClassMember implements SourceCodeRange, JavaSourceNode {
        Type type;
        JavaSourceNode fatherNode;
        RangeString rangeString;
        String head;
        ClassMembers classMembers = new ClassMembers();
        BlockMembers blockMembers = new BlockMembers();
        EnumMembers enumMembers = new EnumMembers();
        AnnotationMembers annotationMembers = new AnnotationMembers();
        FieldMembers fieldMembers = new FieldMembers();
        MethodMembers methodMembers = new MethodMembers();
        JavaDocs javaDocs = new JavaDocs();
        AnnotationUsingMembers annotationUsingMembers = new AnnotationUsingMembers();

        Creator[] creators = {this.classMembers, this.blockMembers, this.enumMembers, this.annotationMembers, this.fieldMembers, this.methodMembers, this.javaDocs, this.annotationUsingMembers};
        List<JavaDocs.JavaDoc> javaDocOverClass = new ArrayList<>();
        List<AnnotationUsingMembers.AnnotationUsingMember> annotationsOverClass = new ArrayList<>();

        public static ClassMembers.ClassMember.Builder builder() {
            return new ClassMember().new Builder();
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
            return type == Type.INTERFACE ? PUBLIC_LEVEL : FRIENDLY_LEVEL;
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
            return JavaSourceNode.javaDocList2String(this.javaDocOverClass);
        }

        @Override
        public int getLocationInFile() {
            return rangeString.from;
        }

        @Override
        public List<JavaSourceNode> getChildren() {
            return JavaSourceNode.mergeList(this.annotationMembers.annotationMemberList, this.classMembers.classMemberList, this.enumMembers.enumMemberList, this.fieldMembers.fieldMemberList, this.methodMembers.methodMemberList);
        }

        @Override
        public JavaSourceNode.Type getType() {
            return type;
        }

        @Override
        public String toString() {
            return StringUtils.toString(StringUtils.list2String(javaDocOverClass), StringUtils.list2String(annotationsOverClass), head, "{", this.fieldMembers, this.blockMembers, this.methodMembers, this.classMembers, this.enumMembers, this.annotationMembers, "}\n");
        }


        class Builder implements MatcherBuilder<ClassMembers.ClassMember> {

            /**
             * rangeString = '其他字符{'
             */
            @Override
            public Pair<ClassMember, String> build(RangeString rangeString, JavaDocs javaDocs, AnnotationUsingMembers annotationUsingMembers, JavaSourceNode fatherNode) {
                String s = StringUtils.prettyString(rangeString);
                RangeString of = RangeString.of(0, s.length(), s);
                if (s.endsWith(JavaKeyWords.LEFT_CURLY_BRACES)) {
                    boolean isClass = StringUtils.checkClassType(of, JavaKeyWords.CLASS);
                    if (isClass || StringUtils.checkClassType(of, JavaKeyWords.INTERFACE)) {
                        ClassMember.this.head = s.substring(0, s.length() - JavaKeyWords.LEFT_CURLY_BRACES.length());
                        ClassMember.this.type = isClass ? Type.CLASS : Type.INTERFACE;
                        ClassMember.this.fatherNode = fatherNode;
                        ClassMember.this.javaDocOverClass = javaDocs.getLastJavaDocList();
                        ClassMember.this.annotationsOverClass = annotationUsingMembers.getLastAnnotationUsingMemberList();
                        Pair<Integer, String> classOrInterface = Creator.createClassOrInterface(rangeString.left(), ClassMember.this.creators, ClassMember.this.javaDocs, ClassMember.this.annotationUsingMembers, ClassMember.this, null);
                        ClassMember.this.rangeString = RangeString.of(rangeString.from, classOrInterface.getFirst(), rangeString.s);

                        return Pair.of(ClassMember.this, classOrInterface.getSecond());
                    } else {
                        return null;
                    }
                }
                return null;
            }
        }


    }

}
