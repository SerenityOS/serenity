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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

public abstract class AbstractBuilder {

    final String name;

    Modifiers modifiers;
    Comment comments;
    String cname;

    /**
     * Constructs the base builder.
     * @param modifiers for the class
     * @param name of the element
     */
    public AbstractBuilder(Modifiers modifiers, String name) {
        this.modifiers = modifiers;
        this.name = name;
        this.comments = new Comment(Comment.Kind.AUTO);
    }

    AbstractBuilder setModifiers(String... mods) {
        this.modifiers = new Modifiers(mods);
        return this;
    }

    /**
     * Sets the enclosing type's name.
     * @param className the enclosing type's name
     */
    void setClassName(String className) {
        this.cname = className;
    }

    /**
     * Sets the comment type for the member.
     * @param comment for the member.
     * @return this builder.
     */
    public AbstractBuilder setComments(Comment comment) {
        this.comments = comment;
        return this;
    }

    /**
     * Sets the comments for the member.
     * @param comments for the member.
     * @return this builder.
     */
    public AbstractBuilder setComments(String... comments) {
        this.comments = new Comment(comments);
        return this;
    }

    /**
     * Sets a comment for the element. Typically used to set
     * user's preferences whether an automatic comment is
     * required or no API comment.
     *
     * @param kind of comment, automatic or no comment.
     * @return this builder.
     */
    public AbstractBuilder setComments(Comment.Kind kind) {
        switch (kind) {
            case NO_API_COMMENT: case AUTO: case INHERIT_DOC:
                this.comments = new Comment(kind);
                break;
            default:
                throw new IllegalArgumentException(kind + " not allowed");
        }
        return this;
    }

    /**
     * The comment container.
     */
    public static class Comment {

        /**
         * The kinds of a comment.
         */
        public enum Kind {
            /**
             * user specified
             */
            USER,
            /**
             * no API comments
             */
            NO_API_COMMENT,
            /**
             * inserts the javadoc tag
             */
            INHERIT_DOC,
            /**
             * auto generate one
             */
            AUTO
        }

        final Kind kind;
        final List<String> comments;

        /**
         * Construct an initial comment.
         *
         * @param kind
         */
        public Comment(Kind kind) {
            this.kind = kind;
            comments = Collections.emptyList();
        }

        /**
         * Specify a user comment.
         *
         * @param comments the string of API comments.
         */
        public Comment(String... comments) {
            kind = Kind.USER;
            this.comments = comments == null
                    ? Collections.emptyList()
                    : List.of(comments);
        }

        @Override
        public String toString() {
            ClassBuilder.OutputWriter ow = new ClassBuilder.OutputWriter();
            switch (kind) {
                case USER:
                    comments.forEach((s) -> ow.println("  " + s));
                    break;
                case INHERIT_DOC:
                    ow.println("{@inheritDoc}");
                    break;
            }
            return ow.toString();
        }
    }

    /**
     * The modifier representation for an element.
     */
    public static class Modifiers {
        List<String> modifiers;

        /**
         * Constructs a modifier container.
         * @param modifiers for an element.
         */
        public Modifiers(String... modifiers) {
            this.modifiers = List.of(modifiers);
        }

        /**
         * Constructs a modifier container.
         * @param modifiers a list of modifier strings.
         */
        public Modifiers(List<String> modifiers) {
            this.modifiers = modifiers;
        }

        /**
         * Sets the modifiers for this element.
         * @param modifiers
         */
        public void setModifiers(String... modifiers) {
            this.modifiers = List.of(modifiers);
        }

        /**
         * Sets the modifiers for this element.
         * @param modifiers
         */
        public void setModifiers(List<String> modifiers) {
            this.modifiers = modifiers;
        }

        @Override
        public String toString() {
            OutputWriter ow = new OutputWriter();
            modifiers.forEach(i -> ow.print(i + " "));
            return ow.toString();
        }
    }

    /**
     * The output writer.
     */
    public static class OutputWriter {
        private final StringWriter sw = new StringWriter();
        private final PrintWriter pw = new PrintWriter(sw);

        @Override
        public String toString() {
            return sw.getBuffer().toString();
        }

        /**
         * Prints a string without NL.
         * @param s the string to print.
         */
        public void print(String s) {
            pw.print(s);
        }

        /**
         * Prints a string with a NL.
         * @param s the string to print.
         */
        public void println(String s) {
            pw.println(s);
        }
    }

    /**
     * A container to encapsulate a pair of values.
     */
    public static class Pair {
        final String first;
        final String second;

        public Pair(String first, String second) {
            this.first = first;
            this.second = second;
        }
    }
}
