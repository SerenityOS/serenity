/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8177466 8189838
 * @summary Add compiler support for local variable type-inference
 * @modules jdk.compiler/com.sun.source.tree
 *          jdk.compiler/com.sun.source.util
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.util
 * @build LocalVariableInferenceTester
 * @run main LocalVariableInferenceTester NonDenotableTest.java
 */
import java.util.List;

class NonDenotableTest {

    static final String OBJECT = "java.lang.Object";
    static final String STRING = "java.lang.String";
    static final String ANON_OBJECT = "#ANON(java.lang.Object)";
    static final String ANON_RUNNABLE = "#ANON(java.lang.Object,java.lang.Runnable)";
    static final String LIST_EXT = "java.util.List<? extends java.lang.String>";
    static final String LIST_SUP = "java.util.List<? super java.lang.String>";
    static final String LIST_UNB = "java.util.List<?>";
    static final String COMP_UNB = "java.lang.Comparable<?>";
    static final String LIST_EXT_COMP_UNB = "java.util.List<? extends java.lang.Comparable<?>>";
    static final String LIST_SUP_COMP_UNB = "java.util.List<? super java.lang.Comparable<?>>";
    static final String INT_INTEGER_DOUBLE = "#INT(java.lang.Number,java.lang.Comparable<? extends java.lang.Number&java.lang.Comparable<?>>)";
    static final String SEL_INT_ENUM_SEL = "NonDenotableTest.Selector<? extends #INT(java.lang.Enum<?>,NonDenotableTest.Selector<?>)>";

    void testExtends() {
        @InferredType(LIST_EXT)
        var s = extString();
        for (@InferredType(LIST_EXT) var s2 = extString() ; ; ) { break; }
        for (@InferredType(LIST_EXT) var s2 : extStringArr()) { break; }
        for (@InferredType(LIST_EXT) var s2 : extStringIter()) { break; }
        for (@InferredType(STRING) var s2 : extString()) { break; }
    }

    void testExtendsFbound() {
        @InferredType(LIST_EXT_COMP_UNB)
        var s = extFbound();
        for (@InferredType(LIST_EXT_COMP_UNB) var s2 = extFbound() ; ; ) { break; }
        for (@InferredType(LIST_EXT_COMP_UNB) var s2 : extFboundArr()) { break; }
        for (@InferredType(LIST_EXT_COMP_UNB) var s2 : extFboundIter()) { break; }
        for (@InferredType(COMP_UNB) var s2 : extFbound()) { break; }
    }

    void testSuperFbound() {
        @InferredType(LIST_UNB)
        var s = supFbound();
        for (@InferredType(LIST_UNB) var s2 = supFbound() ; ; ) { break; }
        for (@InferredType(LIST_UNB) var s2 : supFboundArr()) { break; }
        for (@InferredType(LIST_UNB) var s2 : supFboundIter()) { break; }
        for (@InferredType(OBJECT) var s2 : supFbound()) { break; }
    }

    void testSuper() {
        @InferredType(LIST_SUP)
        var s = supString();
        for (@InferredType(LIST_SUP) var s2 = supString() ; ; ) { break; }
        for (@InferredType(LIST_SUP) var s2 : supStringArr()) { break; }
        for (@InferredType(LIST_SUP) var s2 : supStringIter()) { break; }
        for (@InferredType(OBJECT) var s2 : supString()) { break; }
    }

    void testUnbound() {
        @InferredType(LIST_UNB)
        var s = unbString();
        for (@InferredType(LIST_UNB) var s2 = unbString() ; ; ) { break; }
        for (@InferredType(LIST_UNB) var s2 : unbStringArr()) { break; }
        for (@InferredType(LIST_UNB) var s2 : unbStringIter()) { break; }
        for (@InferredType(OBJECT) var s2 : unbString()) { break; }
    }

    void testAnonymousClass() {
        @InferredType(ANON_OBJECT)
        var o = new Object() { };
        for (@InferredType(ANON_OBJECT) var s2 = new Object() { } ; ; ) { break; }
        for (@InferredType(ANON_OBJECT) var s2 : arrayOf(new Object() { })) { break; }
        for (@InferredType(ANON_OBJECT) var s2 : listOf(new Object() { })) { break; }
    }

    void testAnonymousInterface() {
        @InferredType(ANON_RUNNABLE)
        var r = new Runnable() { public void run() { } };
        for (@InferredType(ANON_RUNNABLE) var s2 = new Runnable() { public void run() { } } ; ; ) { break; }
        for (@InferredType(ANON_RUNNABLE) var s2 : arrayOf(new Runnable() { public void run() { } })) { break; }
        for (@InferredType(ANON_RUNNABLE) var s2 : listOf(new Runnable() { public void run() { } })) { break; }
    }

    void testIntersection() {
        @InferredType(INT_INTEGER_DOUBLE)
        var c = choose(1, 1L);
        for (@InferredType(INT_INTEGER_DOUBLE) var s2 = choose(1, 1L) ; ;) { break; }
        for (@InferredType(INT_INTEGER_DOUBLE) var s2 : arrayOf(choose(1, 1L))) { break; }
        for (@InferredType(INT_INTEGER_DOUBLE) var s2 : listOf(choose(1, 1L))) { break; }
    }

    void testIntersection(Selector<?> s) {
        @InferredType(SEL_INT_ENUM_SEL)
        var c = s;
        for (@InferredType(SEL_INT_ENUM_SEL) var s2 = s ; ;) { break; }
    }

    List<? extends String> extString() { return null; }
    List<? super String> supString() { return null; }
    List<?> unbString() { return null; }

    List<? extends String>[] extStringArr() { return null; }
    List<? super String>[] supStringArr() { return null; }
    List<?>[] unbStringArr() { return null; }

    Iterable<? extends List<? extends String>> extStringIter() { return null; }
    Iterable<? extends List<? super String>> supStringIter() { return null; }
    Iterable<? extends List<?>> unbStringIter() { return null; }

    <Z extends Comparable<Z>> List<? extends Z> extFbound() { return null; }
    <Z extends Comparable<Z>> List<? super Z> supFbound() { return null; }

    <Z extends Comparable<Z>> List<? extends Z>[] extFboundArr() { return null; }
    <Z extends Comparable<Z>> List<? super Z>[] supFboundArr() { return null; }

    <Z extends Comparable<Z>> Iterable<? extends List<? extends Z>> extFboundIter() { return null; }
    <Z extends Comparable<Z>> Iterable<? extends List<? super Z>> supFboundIter() { return null; }

    <Z> List<Z> listOf(Z z) { return null; }
    <Z> Z[] arrayOf(Z z) { return null; }

    <Z> Z choose(Z z1, Z z2) { return z1; }

    interface Selector<E extends Enum<E> & Selector<E>> {}
}
