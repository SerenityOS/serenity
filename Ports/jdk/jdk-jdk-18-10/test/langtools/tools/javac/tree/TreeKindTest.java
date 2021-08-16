/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6341023
 * @summary Tree API: Tree.Kind should have mapping to interface
 * @modules jdk.compiler
 */

import com.sun.source.tree.*;

public class TreeKindTest {
    public static void main(String... args) {
        boolean ok = true;

        for (Tree.Kind k: Tree.Kind.values()) {
            //System.err.println(k + " " + k.asInterface());
            Class<? extends Tree> i = k.asInterface();
            switch (k) {
            case POSTFIX_INCREMENT:
            case POSTFIX_DECREMENT:
            case PREFIX_INCREMENT:
            case PREFIX_DECREMENT:
            case UNARY_PLUS:
            case UNARY_MINUS:
            case BITWISE_COMPLEMENT:
            case LOGICAL_COMPLEMENT:
                ok = ok & verify(k, i, i == UnaryTree.class);
                break;

            case MULTIPLY:
            case DIVIDE:
            case REMAINDER:
            case PLUS:
            case MINUS:
            case LEFT_SHIFT:
            case RIGHT_SHIFT:
            case UNSIGNED_RIGHT_SHIFT:
            case LESS_THAN:
            case GREATER_THAN:
            case LESS_THAN_EQUAL:
            case GREATER_THAN_EQUAL:
            case EQUAL_TO:
            case NOT_EQUAL_TO:
            case AND:
            case XOR:
            case OR:
            case CONDITIONAL_AND:
            case CONDITIONAL_OR:
                ok = ok & verify(k, i, i == BinaryTree.class);
                break;

            case MULTIPLY_ASSIGNMENT:
            case DIVIDE_ASSIGNMENT:
            case REMAINDER_ASSIGNMENT:
            case PLUS_ASSIGNMENT:
            case MINUS_ASSIGNMENT:
            case LEFT_SHIFT_ASSIGNMENT:
            case RIGHT_SHIFT_ASSIGNMENT:
            case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
            case AND_ASSIGNMENT:
            case XOR_ASSIGNMENT:
            case OR_ASSIGNMENT:
                ok = ok & verify(k, i, i == CompoundAssignmentTree.class);
                break;

            case INT_LITERAL:
            case LONG_LITERAL:
            case FLOAT_LITERAL:
            case DOUBLE_LITERAL:
            case BOOLEAN_LITERAL:
            case CHAR_LITERAL:
            case STRING_LITERAL:
            case NULL_LITERAL:
                ok = ok & verify(k, i, i == LiteralTree.class);
                break;

            case UNBOUNDED_WILDCARD:
            case EXTENDS_WILDCARD:
            case SUPER_WILDCARD:
                ok = ok & verify(k, i, i == WildcardTree.class);
                break;

            case INTERFACE:
            case ANNOTATION_TYPE:
            case ENUM:
            case CLASS:
            case RECORD:
                ok = ok & verify(k, i, i == ClassTree.class);
                break;

            case ANNOTATION:
            case TYPE_ANNOTATION:
                ok = ok & verify(k, i, i == AnnotationTree.class);
                break;

            case EXPORTS:
                ok = ok & verify(k, i, i == ExportsTree.class);
                break;

            case OPENS:
                ok = ok & verify(k, i, i == OpensTree.class);
                break;

            case OTHER:
                ok = ok & verify(k, i, i == null);
                break;

            default:
                String ks = k.toString().replace("_", "") + "tree";
                String iName = i.getName();
                String is = iName.substring(iName.lastIndexOf(".") + 1);
                ok = ok & verify(k, i, ks.equalsIgnoreCase(is));
            }
        }

        if (!ok)
            throw new AssertionError("test failed");
    }

    static boolean verify(Tree.Kind k, Class<?> c, boolean b) {
        if (!b)
            System.err.println("error: " + k + " " + c);
        return b;
    }
}
