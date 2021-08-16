/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jshell;

import jdk.jshell.Snippet.Kind;
import jdk.jshell.Snippet.SubKind;

/**
 * The Key is unique for a given signature.  Used internal to the implementation
 * to group signature matched Snippets.  Snippet kinds without a signature
 * (for example expressions) each have their own UniqueKey.
 * @author Robert Field
 */
abstract class Key {

    /**
     * The unique index for this Key
     */
    private final int index;

    /**
     * The JShell corresponding to this Key
     */
    private final JShell state;

    Key(JShell state) {
        this.index = state.nextKeyIndex();
        this.state = state;
    }

    /**
     * The unique numeric identifier for the snippet.  Snippets which replace or
     * redefine existing snippets take on the same key index as the replaced or
     * redefined snippet.
     * The index values are monotonically increasing integers.
     * @return the Key index
     */
    int index() { return index; }

    /**
     * The kind for the key.  Indicates the subinterface of Key.
     * @return the Kind of the Key
     */
    abstract Kind kind();

    /**
     * For foreign key testing.
     */
    JShell state() { return state; }

    /**
     * Grouping for snippets which persist and influence future code.
     * They are keyed off at least the name.  They may be Modified/Replaced
     * with new input.
     */
    static abstract class PersistentKey extends Key {

        private final String name;

        PersistentKey(JShell state, String name) {
            super(state);
            this.name = name;
        }
        /**
         * Name of the snippet.
         *
         * @return the name of the snippet.
         */
        String name() { return name; }
    }

    /**
     * Grouping for snippets which reference declarations
     */
    static abstract class DeclarationKey extends PersistentKey {

        DeclarationKey(JShell state, String name) {
            super(state, name);
        }
    }

    /**
     * Key for a class definition Keys of TYPE_DECL. Keyed off class name.
     */
    static class TypeDeclKey extends DeclarationKey {

        TypeDeclKey(JShell state, String name) {
            super(state, name);
        }

        @Override
        Kind kind() { return Kind.TYPE_DECL; }

        @Override
        public String toString() { return "ClassKey(" + name() + ")#" + index(); }
    }

    /**
     * Key for a method definition Keys of METHOD. Keyed off method name and
     * parameter types.
     */
    static class MethodKey extends DeclarationKey {

        private final String parameterTypes;

        MethodKey(JShell state, String name, String parameterTypes) {
            super(state, name);
            this.parameterTypes = parameterTypes;
        }

        @Override
        Kind kind() { return Kind.METHOD; }

        /**
         * The parameter types of the method. Because of overloading of methods,
         * the parameter types are part of the key.
         *
         * @return a String representation of the parameter types of the method.
         */
        String parameterTypes() { return parameterTypes; }


        @Override
        public String toString() { return "MethodKey(" + name() +
                "(" + parameterTypes() + "))#" + index(); }
    }

    /**
     * Key for a variable definition Keys of VARIABLE. Keyed off variable
     * name.
     */
    static class VarKey extends DeclarationKey {

        VarKey(JShell state, String name) {
            super(state, name);
        }

        @Override
        public Kind kind() { return Kind.VAR; }

        @Override
        public String toString() { return "VariableKey(" + name() + ")#" + index(); }
    }

    /**
     * Key for an import. Keys of IMPORT. Keyed off import text and whether
     * import is static.
     */
    static class ImportKey extends PersistentKey {

        private final SubKind snippetKind;

        ImportKey(JShell state, String name,SubKind snippetKind) {
            super(state, name);
            this.snippetKind = snippetKind;
        }

        @Override
        public Kind kind() { return Kind.IMPORT; }

        /**
         * Which kind of import.
         *
         * @return the appropriate SubKind.
         */
        SubKind snippetKind() {
            return snippetKind;
        }


        @Override
        public String toString() { return "ImportKey(" + name() + "," +
                snippetKind + ")#" + index(); }
    }

    /**
     * Grouping for snippets which are the key for only one input -- even if the
     * exactly same entry is made again. The referenced snippets are thus
     * unmodifiable.
     */
    static abstract class UniqueKey extends Key {

        UniqueKey(JShell state) {
            super(state);
        }
    }

    /**
     * Key for a statement snippet. Keys of STATEMENT. Uniquely keyed, see
     * UniqueKey.
     */
    static class StatementKey extends UniqueKey {

        StatementKey(JShell state) {
            super(state);
        }


        @Override
        public Kind kind() {
            return Kind.STATEMENT;
        }

        @Override
        public String toString() { return "StatementKey#" + index(); }
    }

    /**
     * Key for an expression. Keys of EXPRESSION. Uniquely keyed, see
     * UniqueKey.
     */
    static class ExpressionKey extends UniqueKey {

        private final String name;
        private final String typeName;

        ExpressionKey(JShell state, String name, String typeName) {
            super(state);
            this.name = name;
            this.typeName = typeName;
        }

        @Override
        public Kind kind() { return Kind.EXPRESSION; }


        /**
         * Variable name which is the value of the expression. Since the
         * expression is either just an identifier which is a variable or it is
         * an assignment to a variable, there is always a variable which is the
         * subject of the expression. All other expression snippets become
         * temporary variables referenced by a VariableKey.
         *
         * @return the name of the variable which is the subject of the
         * expression.
         */
        String name() { return name; }

        /**
         * Type of the expression
         *
         * @return String representation of the type of the expression.
         */
        String typeName() { return typeName; }


        @Override
        public String toString() { return "ExpressionKey(" + name() + ")#" + index(); }
    }

    /**
     * Key for an erroneous snippet. Keys of ERRONEOUS. Uniquely keyed, see
     * UniqueKey.
     */
    static class ErroneousKey extends UniqueKey {

        ErroneousKey(JShell state) {
            super(state);
        }

        @Override
        Kind kind() { return Kind.ERRONEOUS; }

        @Override
        public String toString() { return "ErroneousKey#" + index(); }
    }
}
