package top.binggo.repositoryprocessor.bean;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

/**
 * @author binggo
 */
@Data
@Builder
public class JavaDoc {

    private String description;
    @Singular
    private List<Annotation> annotations;
    private JavaDocLocation javaDocLocation;


    @Data
    public static class Annotation {

        private Type type;
        private String target;
        private String description;

        private Annotation(Builder builder) {
            setType(builder.type);
            setTarget(builder.target);
            setDescription(builder.description);
        }

        public static Annotation fromString(String s) {
            String[] split = s.trim().split("\\s+", 3);
            Builder builder = Annotation.newBuilder();
            for (int i = 0; i < split.length; i++) {
                switch (i) {
                    case 0:
                        try {
                            builder.type(Type.of(split[i].trim()));
                        } catch (Exception e) {
                            System.err.println("{" + split[i].trim() + "}");
                            e.printStackTrace();
                        }
                        break;
                    case 1:
                        builder.target(split[i]);
                        break;
                    case 2:
                        builder.description(split[i]);
                        break;
                    default:
                        break;
                }
            }
            return builder.build();
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public static Builder newBuilder(Annotation copy) {
            Builder builder = new Builder();
            builder.type = copy.getType();
            builder.target = copy.getTarget();
            builder.description = copy.getDescription();
            return builder;
        }

        public void setDescription(String description) {
            this.description = description == null ? null : description.replaceAll("\\s+", " ");
        }

        public enum Type {
            /**
             * javadoc中的'@param'注释
             */
            PARAM,
            SPEC,
            PARAMS,
            API_NOTE,
            AUTHOR,
            DATE,
            REVISED,
            DEPRECATED,
            IMPL_NOTE,
            IMPL_SPEC,
            SEE,
            SERIAL,
            SINCE,
            VERSION,
            THROWS,
            EXCEPTION,
            SERIAL_FIELD,
            SERIAL_DATA,
            JLS,
            RETURN,
            OTHER;
            private static final List<String> ARRAY_LIST = new ArrayList<>(values().length);

            static {
                Type[] values = values();
                for (Type value : values) {
                    ARRAY_LIST.add(value.toString());
                }
            }

            public static Type of(String s) {
                StringBuilder stringBuilder = new StringBuilder(s.length());
                for (int i = 0; i < s.length(); i++) {
                    if (Character.isUpperCase(s.charAt(i))) {
                        stringBuilder.append("_");
                    }
                    stringBuilder.append(Character.toUpperCase(s.charAt(i)));
                }
                String name = stringBuilder.toString();
                name = ARRAY_LIST.contains(name) ? name : "OTHER";
                return valueOf(name);
            }

            @Override
            public String toString() {
                String[] s = this.name().toLowerCase().split("_");
                for (int i = 1; i < s.length; i++) {
                    s[i] = Character.toUpperCase(s[i].charAt(0)) + s[i].substring(1);
                }
                return "@" + String.join("", s);
            }
        }

        public static final class Builder {
            private Type type;
            private String target;
            private String description;

            private Builder() {
            }

            public Builder type(Type val) {
                type = val;
                return this;
            }

            public Builder target(String val) {
                target = val;
                return this;
            }

            public Builder description(String val) {
                description = val;
                return this;
            }

            public Annotation build() {
                return new Annotation(this);
            }
        }
    }


    @Builder
    @Data
    public static class JavaDocLocation {
        private String targetName;
        private TargetType targetType;

        public enum TargetType {
            /**
             * 成员变量上面
             */
            FIELD,
            /**
             * 方法上面
             */
            METHOD,
            /**
             * 类上面
             */
            CLASS
        }

    }

}

