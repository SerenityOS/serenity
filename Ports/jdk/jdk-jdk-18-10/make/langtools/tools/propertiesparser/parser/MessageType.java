/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package propertiesparser.parser;

/**
 * Common interface to all kinds of diagnostic argument types.
 */
public interface MessageType {

    /**
     * Visitor method.
     */
    <R, A> R accept(Visitor<R, A> v, A a);

    /**
     * The type as mentioned in the resource file.
     */
    String kindName();

    /**
     * A custom type is a type for which a predefined alternative does not exist. As such, it is an
     * handy option when prototyping - but usages of custom types should be avoided in product-quality
     * resource file comments.
     *
     * Example: 'com.sun.tools.javac.code.Flags.Flag'
     */
    public static class CustomType implements MessageType {

        /** The string-based representation of this type. */
        public String typeString;

        public CustomType(String typeString) {
            this.typeString = typeString;
        }

        @Override
        public String kindName() {
            return typeString;
        }

        @Override
        public <R, A> R accept(Visitor<R, A> v, A a) {
            return v.visitCustomType(this, a);
        }
    }

    /**
     * A predefined type. All common types mentioned in the resource file comments are meant to
     * be included here.
     */
    public enum SimpleType implements MessageType {

        ANNOTATION("annotation", "Compound", "com.sun.tools.javac.code.Attribute"),
        BOOLEAN("boolean", "boolean", null),
        COLLECTION("collection", "Collection", "java.util"),
        FLAG("flag", "Flag", "com.sun.tools.javac.code.Flags"),
        FRAGMENT("fragment", "Fragment", null),
        DIAGNOSTIC("diagnostic", "JCDiagnostic", "com.sun.tools.javac.util"),
        MODIFIER("modifier", "Modifier", "javax.lang.model.element"),
        FILE("file", "File", "java.io"),
        FILE_OBJECT("file object", "JavaFileObject", "javax.tools"),
        PATH("path", "Path", "java.nio.file"),
        NAME("name", "Name", "com.sun.tools.javac.util"),
        NUMBER("number", "int", null),
        OPTION_NAME("option name", "Option", "com.sun.tools.javac.main"),
        PROFILE("profile", "Profile", "com.sun.tools.javac.jvm"),
        SOURCE("source", "Source", "com.sun.tools.javac.code"),
        SOURCE_VERSION("source version", "SourceVersion", "javax.lang.model"),
        STRING("string", "String", null),
        SYMBOL("symbol", "Symbol", "com.sun.tools.javac.code"),
        SYMBOL_KIND("symbol kind", "Kind", "com.sun.tools.javac.code.Kinds"),
        KIND_NAME("kind name", "KindName", "com.sun.tools.javac.code.Kinds"),
        TARGET("target", "Target", "com.sun.tools.javac.jvm"),
        TOKEN("token", "TokenKind", "com.sun.tools.javac.parser.Tokens"),
        TREE_TAG("tree tag", "Tag", "com.sun.tools.javac.tree.JCTree"),
        TYPE("type", "Type", "com.sun.tools.javac.code"),
        URL("url", "URL", "java.net"),
        SET("set", "Set", "java.util"),
        LIST("list", "List", "java.util"),
        OBJECT("object", "Object", null),
        UNUSED("unused", "Void", null),
        UNKNOWN("<unknown>", "UnknownType", null);

        /** name of the predefined type as mentioned in the resource file. */
        public final String kindName;

        /** string-based representation of the type */
        public final String clazz;

        /** type qualifier (might be null) */
        public final String qualifier;

        SimpleType(String kindName, String clazz, String qualifier) {
            this.kindName = kindName;
            this.clazz = clazz;
            this.qualifier = qualifier;
        }

        @Override
        public String kindName() {
            return kindName;
        }

        @Override
        public <R, A> R accept(Visitor<R, A> v, A a) {
            return v.visitSimpleType(this, a);
        }
    }

    /**
     * A compound type is a collection of some element type.
     *
     * Example: list of string
     */
    public static class CompoundType implements MessageType {

        /**
         * Compound type kind.
         */
        public enum Kind {
            COLLECTION("collection of", SimpleType.COLLECTION),
            LIST("list of", SimpleType.LIST),
            SET("set of", SimpleType.SET);

            public final String kindName;
            public final SimpleType clazz;

            Kind(String kindName, SimpleType clazz) {
                this.kindName = kindName;
                this.clazz = clazz;
            }
        }

        /** The compound type kind. */
        public final Kind kind;

        /** The element type. */
        public final MessageType elemtype;

        public CompoundType(Kind kind, MessageType elemtype) {
            this.kind = kind;
            this.elemtype = elemtype;
        }

        @Override
        public String kindName() {
            return kind.kindName;
        }

        @Override
        public <R, A> R accept(Visitor<R, A> v, A a) {
            return v.visitCompoundType(this, a);
        }
    }

    /**
     * A union type represents an alternative between two (or more) types. It can be useful to
     * define the type of an argument which can assume multiple (unrelated) values; union types
     * are only meant to be used in cases where the alternative comes up frequently enough in the
     * resource file comments - in order to avoid cluttered comments.
     *
     * Example: message segment
     */
    public static class UnionType implements MessageType {

        /**
         * Union type kind.
         */
        public enum Kind {
            MESSAGE_SEGMENT("message segment", SimpleType.DIAGNOSTIC, SimpleType.FRAGMENT),
            FILE_NAME("file name", SimpleType.FILE, SimpleType.FILE_OBJECT, SimpleType.PATH);

            final String kindName;
            final SimpleType[] choices;

            Kind(String kindName, SimpleType... choices) {
                this.kindName = kindName;
                this.choices = choices;
            }
        }

        /** The union type kind. */
        public final Kind kind;

        /** The union type alternatives. */
        public final MessageType[] choices;

        UnionType(Kind kind) {
            this(kind, kind.choices);
        }

        protected UnionType(Kind kind, MessageType[] choices) {
            this.choices = choices;
            this.kind = kind;
        }

        @Override
        public String kindName() {
            return kind.kindName;
        }

        @Override
        public <R, A> R accept(Visitor<R, A> v, A a) {
            return v.visitUnionType(this, a);
        }
    }

    /**
     * A subclass of union type representing 'explicit' alternatives in the resource file comments.
     * Note: as the token 'or' is parsed with lowest priority, it is not possible, for instance,
     * to form a compound type out of an 'or' type. In such cases a plain union type should be used
     * instead.
     *
     * Examples: symbol or type
     */
    public static class OrType extends UnionType {

        public static final String OR_NAME = "or";

        @Override
        public String kindName() {
            return OR_NAME;
        }

        public OrType(MessageType... choices) {
            super(null, choices);
        }
    }

    /**
     * Visitor class.
     */
    public static abstract class Visitor<R, A> {
        public abstract R visitCustomType(CustomType t, A a);
        public abstract R visitSimpleType(SimpleType t, A a);
        public abstract R visitCompoundType(CompoundType t, A a);
        public abstract R visitUnionType(UnionType t, A a);
    }
}
