/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package builder;

import toolbox.ToolBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builder for type declarations.
 * Note: this implementation does not support everything and is not
 * exhaustive.
 */
public class ClassBuilder extends AbstractBuilder {

    private final ToolBox tb;
    private final String fqn;
    private final String clsname;
    private final String typeParameter;

    private String pkg;
    private final List<String> imports;

    private String extendsType;
    private final List<String> implementsTypes;
    private final List<MemberBuilder> members;
    private final List<ClassBuilder> inners;
    private final List<ClassBuilder> nested;


    final static Pattern CLASS_RE = Pattern.compile("(.*)(<.*>)");

    /**
     * Creates a class builder.
     * @param tb the toolbox reference
     * @param name the name of the type
     */
    public ClassBuilder(ToolBox tb, String name) {
        super(new Modifiers(), name);
        this.tb = tb;

        Matcher m = CLASS_RE.matcher(name);
        if (m.matches()) {
            fqn = m.group(1);
            typeParameter = m.group(2);
        } else {
            fqn = name;
            typeParameter = null;
        }
        if (fqn.contains(".")) {
            this.pkg = name.substring(0, fqn.lastIndexOf('.'));
            clsname = fqn.substring(fqn.lastIndexOf('.') + 1);
        } else {
            clsname = fqn;
        }
        imports = new ArrayList<>();
        implementsTypes = new ArrayList<>();
        members = new ArrayList<>();
        nested = new ArrayList<>();
        inners = new ArrayList<>();
    }

    /**
     * Adds an import(s).
     * @param i the import type.
     * @return this builder.
     */
    public ClassBuilder addImports(String i) {
        imports.add(i);
        return this;
    }

    /**
     * Sets the modifiers for this builder.
     * @param modifiers the modifiers
     * @return this builder
     */
    public ClassBuilder setModifiers(String... modifiers) {
        this.modifiers.setModifiers(modifiers);
        return this;
    }

    /**
     * Sets the enclosing type's name.
     *
     * @param className the enclosing type's name
     */
    @Override
    void setClassName(String className) {
        cname = className;
    }

    /**
     * Sets a comment for the element.
     *
     * @param comments for the element
     * @return this builder.
     */
    @Override
    public ClassBuilder setComments(String... comments) {
        super.setComments(comments);
        return this;
    }

    /**
     * Sets a comment for the element. Typically used to set
     * the user's preferences whether an automatic comment is
     * required or no API comment.
     *
     * @param kind of comment, automatic or no comment.
     * @return this builder.
     */
    @Override
    public ClassBuilder setComments(Comment.Kind kind) {
        super.setComments(kind);
        return this;
    }

    /**
     * Set the super-type of the type.
     * @param name of the super type.
     * @return this builder.
     */
    public ClassBuilder setExtends(String name) {
        extendsType = name;
        return this;
    }

    /**
     * Adds an implements declaration(s).
     * @param names the interfaces
     * @return this builder.
     */
    public ClassBuilder addImplements(String... names) {
        implementsTypes.addAll(List.of(names));
        return this;
    }

    /**
     * Adds a member(s) to the class declaration.
     * @param mbs the member builder(s) representing member(s).
     * @return this builder
     */
    public ClassBuilder addMembers(MemberBuilder... mbs) {
        for (MemberBuilder mb : mbs) {
            members.add(mb);
            mb.setClassName(fqn);
        }
        return this;
    }

    /**
     * Adds nested-classes, to an outer class to an outer builder.
     * @param cbs class builder(s) of the nested classes.
     * @return this builder.
     */
    public ClassBuilder addNestedClasses(ClassBuilder... cbs) {
        Stream.of(cbs).forEach(cb -> {
            nested.add(cb);
            cb.setClassName(fqn);
        });
        return this;
    }

    /**
     * Adds inner-classes, to an an outer class builder.
     * @param cbs class builder(s) of the inner classes.
     * @return this builder.
     */
    public ClassBuilder addInnerClasses(ClassBuilder... cbs) {
        Stream.of(cbs).forEach(cb -> {
            inners.add(cb);
            cb.setClassName(fqn);
        });
        return this;
    }

    @Override
    public String toString() {
        OutputWriter ow = new OutputWriter();
        if (pkg != null)
            ow.println("package " + pkg + ";");
        imports.forEach(i -> ow.println("import " + i + ";"));
        switch (comments.kind) {
            case AUTO:
                ow.println("/** Class " + fqn + " */");
                break;
            case USER:
                ow.println("/** ");
                comments.comments.forEach(c -> ow.println(" * " + c));
                ow.println(" */");
                break;
            case NO_API_COMMENT:
                ow.println("// NO_API_COMMENT");
                break;
        }
        ow.print(modifiers.toString());
        ow.print(clsname);
        if (typeParameter != null) {
            ow.print(typeParameter + " ");
        } else {
            ow.print(" ");
        }
        if (extendsType != null && !extendsType.isEmpty()) {
            ow.print("extends " + extendsType + " ");
        }
        if (!implementsTypes.isEmpty()) {
            ow.print("implements ");

            ListIterator<String> iter = implementsTypes.listIterator();
            while (iter.hasNext()) {
                String s = iter.next() ;
                ow.print(s);
                if (iter.hasNext())
                    ow.print(", ");
            }
        }
        ow.print("{");
        if (!nested.isEmpty()) {
            ow.println("");
            nested.forEach(m -> ow.println(m.toString()));
        }

        if (!members.isEmpty()) {
            ow.println("");
            members.forEach(m -> ow.println(m.toString()));
        }

        ow.println("}");
        if (!inners.isEmpty()) {
            ow.println(" {");
            inners.forEach(m -> ow.println(m.toString()));
            ow.println("}");
        }
        return ow.toString();
    }

    /**
     * Writes out the java source for a type element. Package directories
     * will be created as needed as inferred by the type name.
     * @param srcDir the top level source directory.
     * @throws IOException if an error occurs.
     */
    public void write(Path srcDir) throws IOException {
        Files.createDirectories(srcDir);
        Path outDir = srcDir;
        if (pkg != null && !pkg.isEmpty()) {
            String pdir = pkg.replace(".", "/");
            outDir = Paths.get(srcDir.toString(), pdir);
            Files.createDirectories(outDir);
        }
        Path filePath = Paths.get(outDir.toString(), clsname + ".java");
        tb.writeFile(filePath, this.toString());
    }

    /**
     * The member builder, this is the base class for all types of members.
     */
    public static abstract class MemberBuilder extends AbstractBuilder {
        public MemberBuilder(Modifiers modifiers, String name) {
            super(modifiers, name);
        }

        /**
         * Sets the enclosing type's name.
         * @param className the enclosing type's name
         */
        @Override
        void setClassName(String className) {
            cname = className;
        }

        /**
         * Sets a comment for the element.
         *
         * @param comments for any element
         * @return this builder.
         */
        @Override
        public MemberBuilder setComments(String... comments) {
            super.setComments(comments);
            return this;
        }

        /**
         * Sets a comment for the element. Typically used to set user's
         * preferences whether an automatic comment is required or no API
         * comment.
         *
         * @param kind of comment, automatic or no comment.
         * @return this builder.
         */
        @Override
        public MemberBuilder setComments(Comment.Kind kind) {
            super.setComments(kind);
            return this;
        }

        /**
         * Sets a new modifier.
         *
         * @param modifiers
         * @return this builder.
         */
        @Override
        public MemberBuilder setModifiers(String... modifiers) {
            super.setModifiers(modifiers);
            return this;
        }
    }

    /**
     * The field builder.
     */
    public static class FieldBuilder extends MemberBuilder {
        private String fieldType;
        private String value;

        private static final Pattern FIELD_RE = Pattern.compile("(.*)(\\s*=\\s*)(.*)(;)");

        /**
         * Constructs a field with the modifiers and name of the field.
         * The builder by default is configured to auto generate the
         * comments for the field.
         * @param name of the field
         */
        public FieldBuilder(String name) {
            super(new Modifiers(), name);
            this.comments = new Comment(Comment.Kind.AUTO);
        }

        /**
         * Returns a field builder by parsing the string.
         * ex:  public static String myPlayingField;
         * @param fieldString describing the field.
         * @return a field builder.
         */
        public static FieldBuilder parse(String fieldString) {
            String prefix;
            String value = null;
            Matcher m = FIELD_RE.matcher(fieldString);
            if (m.matches()) {
                prefix = m.group(1).trim();
                value = m.group(3).trim();
            } else {
                int end = fieldString.lastIndexOf(';') > 0
                        ? fieldString.lastIndexOf(';')
                        : fieldString.length();
                prefix = fieldString.substring(0, end).trim();
            }
            List<String> list = Stream.of(prefix.split(" "))
                    .filter(s -> !s.isEmpty()).collect(Collectors.toList());
            if (list.size() < 2) {
                throw new IllegalArgumentException("incorrect field string: "
                        + fieldString);
            }
            String name = list.get(list.size() - 1);
            String fieldType = list.get(list.size() - 2);

            FieldBuilder fb = new FieldBuilder(name);
            fb.modifiers.setModifiers(list.subList(0, list.size() - 2));
            fb.setFieldType(fieldType);
            if (value != null)
                fb.setValue(value);

            return fb;
        }

        /**
         * Sets the modifiers for this builder.
         *
         * @param mods
         * @return this builder
         */
        public FieldBuilder setModifiers(String mods) {
            this.modifiers.setModifiers(mods);
            return this;
        }

        /**
         * Sets the type of the field.
         * @param fieldType the name of the type.
         * @return this field builder.
         */
        public FieldBuilder setFieldType(String fieldType) {
            this.fieldType = fieldType;
            return this;
        }

        public FieldBuilder setValue(String value) {
            this.value = value;
            return this;
        }

        @Override
        public String toString() {
            String indent = "    ";
            OutputWriter ow = new OutputWriter();
            switch (comments.kind) {
                case AUTO:
                    ow.println(indent + "/** Field " +
                            super.name + " in " + super.cname + " */");
                    break;
                case INHERIT_DOC: case USER:
                    ow.println(indent + "/** " +
                            comments.toString() + " */");
                    break;
                case NO_API_COMMENT:
                    ow.println(indent + "// NO_API_COMMENT");
                    break;
            }
            ow.print(indent + super.modifiers.toString() + " ");
            ow.print(fieldType + " ");
            ow.print(super.name);
            if (value != null) {
                ow.print(" = " + value);
            }
            ow.println(";");
            return ow.toString();
        }
    }

    /**
     * The method builder.
     */
    public static class MethodBuilder extends MemberBuilder {

        private final List<Pair> params;

        private String returnType;
        private List<String> body;

        final static Pattern METHOD_RE = Pattern.compile("(.*)(\\()(.*)(\\))(.*)");

        /**
         * Constructs a method builder. The builder by default is configured
         * to auto generate the comments for this method.
         * @param name of the method.
         */
        public MethodBuilder(String name) {
            super(new Modifiers(), name);
            comments = new Comment(Comment.Kind.AUTO);
            params = new ArrayList<>();
            body = null;
        }

        /**
         * Returns a method builder by parsing a string which
         * describes a method.
         * @param methodString the method description.
         * @return a method builder.
         */
        public static MethodBuilder parse(String methodString) {
            Matcher m = METHOD_RE.matcher(methodString);
            if (!m.matches())
                throw new IllegalArgumentException("string does not match: "
                        + methodString);
            String prefix = m.group(1);
            String params = m.group(3);
            String suffix = m.group(5).trim();

            if (prefix.length() < 2) {
                throw new IllegalArgumentException("incorrect method string: "
                        + methodString);
            }

            String[] pa = prefix.split(" ");
            List<String> list = List.of(pa);
            String name = list.get(list.size() - 1);
            String returnType = list.get(list.size() - 2);

            MethodBuilder mb = new MethodBuilder(name);
            mb.modifiers.setModifiers(list.subList(0, list.size() - 2));
            mb.setReturn(returnType);

            pa = params.split(",");
            Stream.of(pa).forEach(p -> {
                p = p.trim();
                if (!p.isEmpty())
                    mb.addParameter(p);
            });
            if (!suffix.isEmpty() || suffix.length() > 1) {
                mb.setBody(suffix);
            }
            return mb;
        }

        /**
         * Sets the modifiers for this builder.
         *
         * @param modifiers
         * @return this builder
         */
        public MethodBuilder setModifiers(String modifiers) {
            this.modifiers.setModifiers(modifiers);
            return this;
        }

        @Override
        public MethodBuilder setComments(String... comments) {
            super.setComments(comments);
            return this;
        }

        @Override
        public MethodBuilder setComments(Comment.Kind kind) {
            super.setComments(kind);
            return this;
        }

        /**
         * Sets a return type for a method.
         * @param returnType the return type.
         * @return this method builder.
         */
        public MethodBuilder setReturn(String returnType) {
            this.returnType = returnType;
            return this;
        }

        /**
         * Adds a parameter(s) to the method method builder.
         * @param params a pair consisting of type and parameter name.
         * @return this method builder.
         */
        public MethodBuilder addParameters(Pair... params) {
            this.params.addAll(List.of(params));
            return this;
        }

        /**
         * Adds a parameter to the method method builder.
         * @param type the type of parameter.
         * @param name the parameter name.
         * @return this method builder.
         */
        public MethodBuilder addParameter(String type, String name) {
            this.params.add(new Pair(type, name));
            return this;
        }

        /**
         * Adds a parameter to the method builder, by parsing the string.
         * @param s the parameter description such as "Double voltage"
         * @return this method builder.
         */
        public MethodBuilder addParameter(String s) {
            String[] p = s.trim().split(" ");
            return addParameter(p[0], p[p.length - 1]);
        }

        /**
         * Sets the body of the method, described by the string.
         * Such as "{", "double i = v/r;", "return i;", "}"
         * @param body of the methods
         * @return
         */
        public MethodBuilder setBody(String... body) {
            if (body == null) {
                this.body = null;
            } else {
                this.body = new ArrayList<>();
                this.body.addAll(List.of(body));
            }
            return this;
        }

        @Override
        public String toString() {
            OutputWriter ow = new OutputWriter();
            String indent = "    ";
            switch (comments.kind) {
                case AUTO:
                    ow.println(indent + "/** Method " + super.name + " in " + super.cname);
                    if (!params.isEmpty())
                        params.forEach(p -> ow.println(indent + " * @param " + p.second + " a param"));
                    if (returnType != null && !returnType.isEmpty() && !returnType.contains("void"))
                        ow.println(indent + " * @return returns something");
                    ow.println(indent + " */");
                    break;
                case INHERIT_DOC: case USER:
                    ow.println(indent + "/** " + comments.toString() + " */");
                    break;
                case NO_API_COMMENT:
                    ow.println(indent + "// NO_API_COMMENT");
                    break;
            }

            ow.print(indent + super.modifiers.toString() + " ");
            ow.print(returnType + " ");
            ow.print(super.name + "(");
            if (!params.isEmpty()) {
                ListIterator<Pair> iter = params.listIterator();
                while (iter.hasNext()) {
                    Pair p = iter.next();
                    ow.print(p.first + " " + p.second);
                    if (iter.hasNext())
                        ow.print(", ");
                }
            }
            ow.print(")");
            if (body == null) {
                ow.println(";");
            } else {
                body.forEach(ow::println);
            }
            return ow.toString();
        }
    }

//A sample, to test with an IDE.
//    public static void main(String... args) throws IOException {
//        ClassBuilder cb = new ClassBuilder(new ToolBox(), "foo.bar.Test<C extends A>");
//        cb.addModifiers("public", "abstract", "static")
//                .addImports("java.io").addImports("java.nio")
//                .setComments("A comment")
//                .addImplements("Serialization", "Something")
//                .setExtends("java.lang.Object")
//                .addMembers(
//                        FieldBuilder.parse("public int xxx;"),
//                        FieldBuilder.parse("public int yyy = 10;"),
//                        MethodBuilder.parse("public static void main(A a, B b, C c);")
//                            .setComments("CC"),
//                        MethodBuilder.parse("void foo(){//do something}")
//
//                );
//        ClassBuilder ic = new ClassBuilder(new ToolBox(), "IC");
//        cb.addModifiers( "interface");
//        cb.addNestedClasses(ic);
//        System.out.println(cb.toString());
//        cb.write(Paths.get("src-out"));
//    }
}
