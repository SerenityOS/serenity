/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8049238
 * @summary Checks Signature attribute for method parameters.
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @build MethodParameterTest Driver ExpectedSignature ExpectedSignatureContainer
 * @run main Driver MethodParameterTest
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ExpectedSignature(descriptor = "MethodParameterTest", signature = "<T:Ljava/lang/Object;>Ljava/lang/Object;")
public class MethodParameterTest<T> {

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, java.util.Set, java.util.ArrayList)",
            signature = "<E:TT;U::Ljava/util/List<TE;>;:Ljava/lang/Runnable;>" +
                    "(TT;Ljava/util/Set<-TE;>;Ljava/util/ArrayList<+Ljava/util/Set<TU;>;>;)V")
    <E extends T, U extends List<E> & Runnable>
    MethodParameterTest(T a, Set<? super E> b, ArrayList<? extends Set<U>> c) {
    }

    @ExpectedSignature(descriptor = "method(java.lang.Object, java.util.List, java.util.ArrayList)",
            signature = "<E:TT;U::Ljava/util/List<TE;>;:Ljava/lang/Runnable;>" +
                    "(TT;TU;Ljava/util/ArrayList<+Ljava/util/Set<TU;>;>;)V")
    <E extends T, U extends List<E> & Runnable>
    void method(T a, U b, ArrayList<? extends Set<U>> c) {
    }

    @ExpectedSignature(descriptor = "staticMethod(java.util.Set, java.util.List[], java.util.Map)",
            signature = "<T::Ljava/util/List<*>;E::Ljava/util/Set<-TT;>;>(TE;[TT;Ljava/util/Map<*+TE;>;)TE;")
    static <T extends List<?>, E extends Set<? super T>> E staticMethod(E a, T[] b, Map<?, ? extends E> c) {
        return null;
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, boolean[])", signature = "(TT;[Z)V")
    MethodParameterTest(T a, boolean...b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, char[])", signature = "(TT;[C)V")
    MethodParameterTest(T a, char...b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, byte[])", signature = "(TT;[B)V")
    MethodParameterTest(T a, byte...b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, short[])", signature = "(TT;[S)V")
    MethodParameterTest(T a, short...b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, int[])", signature = "(TT;[I)V")
    MethodParameterTest(T a, int...b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, long[])", signature = "(TT;[J)V")
    MethodParameterTest(T a, long...b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, float[])", signature = "(TT;[F)V")
    MethodParameterTest(T a, float...b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, double[])", signature = "(TT;[D)V")
    MethodParameterTest(T a, double...b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, java.lang.Object[])",
            signature = "(TT;[Ljava/lang/Object;)V")
    MethodParameterTest(T a, Object...b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object[])", signature = "([TT;)V")
    MethodParameterTest(T...a) {
    }

    // no Signature attribute
    MethodParameterTest(int...a) {
    }

    @ExpectedSignature(descriptor = "genericBooleanMethod(java.lang.Object, boolean[])", signature = "(TT;[Z)V")
    void genericBooleanMethod(T a, boolean...b) {
    }

    @ExpectedSignature(descriptor = "genericCharMethod(java.lang.Object, char[])", signature = "(TT;[C)V")
    void genericCharMethod(T a, char...b) {
    }

    @ExpectedSignature(descriptor = "genericByteMethod(java.lang.Object, byte[])", signature = "(TT;[B)V")
    void genericByteMethod(T a, byte...b) {
    }

    @ExpectedSignature(descriptor = "genericShortMethod(java.lang.Object, short[])", signature = "(TT;[S)V")
    void genericShortMethod(T a, short...b) {
    }

    @ExpectedSignature(descriptor = "genericIntMethod(java.lang.Object, int[])", signature = "(TT;[I)V")
    void genericIntMethod(T a, int...b) {
    }

    @ExpectedSignature(descriptor = "genericLongMethod(java.lang.Object, long[])", signature = "(TT;[J)V")
    void genericLongMethod(T a, long...b) {
    }

    @ExpectedSignature(descriptor = "genericFloatMethod(java.lang.Object, float[])", signature = "(TT;[F)V")
    void genericFloatMethod(T a, float...b) {
    }

    @ExpectedSignature(descriptor = "genericDoubleMethod(java.lang.Object, double[])", signature = "(TT;[D)V")
    void genericDoubleMethod(T a, double...b) {
    }

    @ExpectedSignature(descriptor = "genericObjectMethod(java.lang.Object, java.lang.Object[])",
            signature = "(TT;[Ljava/lang/Object;)V")
    void genericObjectMethod(T a, Object...b) {
    }

    @ExpectedSignature(descriptor = "genericMethod(java.lang.Object[])", signature = "([TT;)V")
    void genericMethod(T...a) {
    }

    void noSignature(int...a) {
    }

    @ExpectedSignature(descriptor = "staticGenericBooleanMethod(java.lang.Object, boolean[])",
            signature = "<T:Ljava/lang/Object;>(TT;[Z)V")
    static <T> void staticGenericBooleanMethod(T a, boolean...b) {
    }

    @ExpectedSignature(descriptor = "staticGenericCharMethod(java.lang.Object, char[])",
            signature = "<T:Ljava/lang/Object;>(TT;[C)V")
    static <T> void staticGenericCharMethod(T a, char...b) {
    }

    @ExpectedSignature(descriptor = "staticGenericByteMethod(java.lang.Object, byte[])",
            signature = "<T:Ljava/lang/Object;>(TT;[B)V")
    static <T> void staticGenericByteMethod(T a, byte...b) {
    }

    @ExpectedSignature(descriptor = "staticGenericShortMethod(java.lang.Object, short[])",
            signature = "<T:Ljava/lang/Object;>(TT;[S)V")
    static <T> void staticGenericShortMethod(T a, short...b) {
    }

    @ExpectedSignature(descriptor = "staticGenericIntMethod(java.lang.Object, int[])",
            signature = "<T:Ljava/lang/Object;>(TT;[I)V")
    static <T> void staticGenericIntMethod(T a, int...b) {
    }

    @ExpectedSignature(descriptor = "staticGenericLongMethod(java.lang.Object, long[])",
            signature = "<T:Ljava/lang/Object;>(TT;[J)V")
    static <T> void staticGenericLongMethod(T a, long...b) {
    }

    @ExpectedSignature(descriptor = "staticGenericFloatMethod(java.lang.Object, float[])",
            signature = "<T:Ljava/lang/Object;>(TT;[F)V")
    static <T> void staticGenericFloatMethod(T a, float...b) {
    }

    @ExpectedSignature(descriptor = "staticGenericDoubleMethod(java.lang.Object, double[])",
            signature = "<T:Ljava/lang/Object;>(TT;[D)V")
    static <T> void staticGenericDoubleMethod(T a, double...b) {
    }

    @ExpectedSignature(descriptor = "staticGenericObjectMethod(java.lang.Object, java.lang.Object[])",
            signature = "<T:Ljava/lang/Object;>(TT;[Ljava/lang/Object;)V")
    static <T> void staticGenericObjectMethod(T a, Object...b) {
    }

    @ExpectedSignature(descriptor = "staticGenericMethod(java.lang.Object[])",
            signature = "<T:Ljava/lang/Object;>([TT;)V")
    static <T> void staticGenericMethod(T...a) {
    }

    static void staticNoSignatureAttribute(int...a) {
    }
}
