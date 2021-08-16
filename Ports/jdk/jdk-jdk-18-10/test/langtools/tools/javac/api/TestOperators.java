/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6338064 6346249 6340951 6392177
 * @summary Tree API: can't determine kind of operator
 * @author  Peter von der Ah\u00e9
 * @library ../lib
 * @modules jdk.compiler
 * @build JavacTestingAbstractProcessor TestOperators
 * @compile -processor TestOperators -proc:only TestOperators.java
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import static javax.tools.Diagnostic.Kind.*;

import com.sun.source.tree.*;
import com.sun.source.util.Trees;

import static com.sun.source.tree.Tree.Kind.*;

@interface TestMe {
    Tree.Kind value();
}

@SupportedAnnotationTypes("TestMe")
public class TestOperators extends JavacTestingAbstractProcessor {

    @TestMe(POSTFIX_INCREMENT)
    public int test_POSTFIX_INCREMENT(int i) {
        return i++;
    }

    @TestMe(POSTFIX_DECREMENT)
    public int test_POSTFIX_DECREMENT(int i) {
        return i--;
    }

    @TestMe(PREFIX_INCREMENT)
    public int test_PREFIX_INCREMENT(int i) {
        return ++i;
    }

    @TestMe(PREFIX_DECREMENT)
    public int test_PREFIX_DECREMENT(int i) {
        return --i;
    }

    @TestMe(UNARY_PLUS)
    public int test_UNARY_PLUS(int i) {
        return +i;
    }

    @TestMe(UNARY_MINUS)
    public int test_UNARY_MINUS(int i) {
        return -i;
    }

    @TestMe(BITWISE_COMPLEMENT)
    public int test_BITWISE_COMPLEMENT(int i) {
        return ~i;
    }

    @TestMe(LOGICAL_COMPLEMENT)
    public boolean test_LOGICAL_COMPLEMENT(boolean b) {
        return !b;
    }

    @TestMe(MULTIPLY)
    public int test_MULTIPLY(int i, int j) {
        return i * j;
    }

    @TestMe(DIVIDE)
    public int test_DIVIDE(int i, int j) {
        return i / j;
    }

    @TestMe(REMAINDER)
    public int test_REMAINDER(int i, int j) {
        return i % j;
    }

    @TestMe(PLUS)
    public int test_PLUS(int i, int j) {
        return i + j;
    }

    @TestMe(MINUS)
    public int test_MINUS(int i, int j) {
        return i - j;
    }

    @TestMe(LEFT_SHIFT)
    public int test_LEFT_SHIFT(int i, int j) {
        return i << j;
    }

    @TestMe(RIGHT_SHIFT)
    public int test_RIGHT_SHIFT(int i, int j) {
        return i >> j;
    }

    @TestMe(UNSIGNED_RIGHT_SHIFT)
    public int test_UNSIGNED_RIGHT_SHIFT(int i, int j) {
        return i >>> j;
    }

    @TestMe(LESS_THAN)
    public boolean test_LESS_THAN(int i, int j) {
        return i < j;
    }

    @TestMe(GREATER_THAN)
    public boolean test_GREATER_THAN(int i, int j) {
        return i > j;
    }

    @TestMe(LESS_THAN_EQUAL)
    public boolean test_LESS_THAN_EQUAL(int i, int j) {
        return i <= j;
    }

    @TestMe(GREATER_THAN_EQUAL)
    public boolean test_GREATER_THAN_EQUAL(int i, int j) {
        return i >= j;
    }

    @TestMe(EQUAL_TO)
    public boolean test_EQUAL_TO(int i, int j) {
        return i == j;
    }

    @TestMe(NOT_EQUAL_TO)
    public boolean test_NOT_EQUAL_TO(int i, int j) {
        return i != j;
    }

    @TestMe(AND)
    public boolean test_AND(boolean a, boolean b) {
        return a & b;
    }

    @TestMe(XOR)
    public boolean test_XOR(boolean a, boolean b) {
        return a ^ b;
    }

    @TestMe(OR)
    public boolean test_OR(boolean a, boolean b) {
        return a | b;
    }

    @TestMe(CONDITIONAL_AND)
    public boolean test_CONDITIONAL_AND(boolean a, boolean b) {
        return a && b;
    }

    @TestMe(CONDITIONAL_OR)
    public boolean test_CONDITIONAL_OR(boolean a, boolean b) {
        return a || b;
    }

    @TestMe(MULTIPLY_ASSIGNMENT)
    public int test_MULTIPLY_ASSIGNMENT(int i, int j) {
        return i *= j;
    }

    @TestMe(DIVIDE_ASSIGNMENT)
    public int test_DIVIDE_ASSIGNMENT(int i, int j) {
        return i /= j;
    }

    @TestMe(REMAINDER_ASSIGNMENT)
    public int test_REMAINDER_ASSIGNMENT(int i, int j) {
        return i %= j;
    }

    @TestMe(PLUS_ASSIGNMENT)
    public int test_PLUS_ASSIGNMENT(int i, int j) {
        return i += j;
    }

    @TestMe(MINUS_ASSIGNMENT)
    public int test_MINUS_ASSIGNMENT(int i, int j) {
        return i -= j;
    }

    @TestMe(LEFT_SHIFT_ASSIGNMENT)
    public int test_LEFT_SHIFT_ASSIGNMENT(int i, int j) {
        return i <<= j;
    }

    @TestMe(RIGHT_SHIFT_ASSIGNMENT)
    public int test_RIGHT_SHIFT_ASSIGNMENT(int i, int j) {
        return i >>= j;
    }

    @TestMe(UNSIGNED_RIGHT_SHIFT_ASSIGNMENT)
    public int test_UNSIGNED_RIGHT_SHIFT_ASSIGNMENT(int i, int j) {
        return i >>>= j;
    }

    @TestMe(AND_ASSIGNMENT)
    public boolean test_AND_ASSIGNMENT(boolean a, boolean b) {
        return a &= b;
    }

    @TestMe(XOR_ASSIGNMENT)
    public boolean test_XOR_ASSIGNMENT(boolean a, boolean b) {
        return a ^= b;
    }

    @TestMe(OR_ASSIGNMENT)
    public boolean test_OR_ASSIGNMENT(boolean a, boolean b) {
        return a |= b;
    }

    @TestMe(INT_LITERAL)
    public Object test_INT_LITERAL() {
        return 0;
    }

    @TestMe(LONG_LITERAL)
    public Object test_LONG_LITERAL() {
        return 0L;
    }

    @TestMe(FLOAT_LITERAL)
    public Object test_FLOAT_LITERAL() {
        return 0.0F;
    }

    @TestMe(DOUBLE_LITERAL)
    public Object test_DOUBLE_LITERAL() {
        return 0.0;
    }

    @TestMe(BOOLEAN_LITERAL)
    public Object test_BOOLEAN_LITERAL() {
        return true;
    }

    @TestMe(CHAR_LITERAL)
    public Object test_CHAR_LITERAL() {
        return 'a';
    }

    @TestMe(STRING_LITERAL)
    public Object test_STRING_LITERAL() {
        return "a";
    }

    @TestMe(NULL_LITERAL)
    public Object test_NULL_LITERAL() {
        return null;
    }

    @TestMe(UNBOUNDED_WILDCARD)
    public Set<?> test_UNBOUNDED_WILDCARD() {
        return null;
    }

    @TestMe(EXTENDS_WILDCARD)
    public Set<? extends Number> test_EXTENDS_WILDCARD() {
        return null;
    }

    @TestMe(SUPER_WILDCARD)
    public Set<? super Number> test_SUPER_WILDCARD() {
        return null;
    }

    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnvironment)
    {
        final Trees trees = Trees.instance(processingEnv);
        final Messager log = processingEnv.getMessager();
        final Elements elements = processingEnv.getElementUtils();
        class Scan extends ElementScanner<Void,Void> {
            @Override
            public Void visitExecutable(ExecutableElement e, Void p) {
                Object debug = e; // info for exception handler
                try {
                    TestMe info = e.getAnnotation(TestMe.class);
                    if (info == null)
                        return null;

                    Tree.Kind kind = info.value();
                    MethodTree node = trees.getTree(e);
                    debug = node;
                    Tree testNode;
                    switch (kind) {
                    case UNBOUNDED_WILDCARD:
                    case EXTENDS_WILDCARD:
                    case SUPER_WILDCARD:
                        ParameterizedTypeTree typeTree;
                        typeTree = (ParameterizedTypeTree) node.getReturnType();
                        testNode = typeTree.getTypeArguments().get(0);
                        break;
                    default:
                        ReturnTree returnNode;
                        returnNode = (ReturnTree) node.getBody().getStatements().get(0);
                        testNode = returnNode.getExpression();
                    }
                    if (testNode.getKind() != kind) {
                        log.printMessage(ERROR, testNode.getKind() + " != " + kind, e);
                        throw new AssertionError(testNode);
                    }
                    System.err.format("OK: %32s %s%n", kind, testNode);
                } catch (Error ex) {
                    System.err.println("Error while looking at " + debug);
                    throw ex;
                }
                return null;
            }
        }
        Scan scan = new Scan();
        for (Element e : roundEnvironment.getRootElements()) {
            scan.scan(e);
        }
        return true;
    }
}
