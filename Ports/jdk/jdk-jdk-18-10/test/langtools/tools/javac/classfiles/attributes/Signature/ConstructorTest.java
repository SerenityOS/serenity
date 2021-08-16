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
 * @summary Checks Signature attribute for constructors.
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @build ConstructorTest Driver ExpectedSignature ExpectedSignatureContainer
 * @run main Driver ConstructorTest
 */

import java.lang.ref.ReferenceQueue;
import java.util.*;

@ExpectedSignature(descriptor = "ConstructorTest", signature = "<T:Ljava/lang/Object;>Ljava/lang/Object;")
public class ConstructorTest<T> {

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, boolean)", signature = "(TT;Z)V")
    ConstructorTest(T a, boolean b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, byte)", signature = "(TT;B)V")
    ConstructorTest(T a, byte b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, char)", signature = "(TT;C)V")
    ConstructorTest(T a, char b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, short)", signature = "(TT;S)V")
    ConstructorTest(T a, short b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, int)", signature = "(TT;I)V")
    ConstructorTest(T a, int b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, long)", signature = "(TT;J)V")
    ConstructorTest(T a, long b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, float)", signature = "(TT;F)V")
    ConstructorTest(T a, float b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, double)", signature = "(TT;D)V")
    ConstructorTest(T a, double b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, java.lang.Runnable)", signature = "(TT;Ljava/lang/Runnable;)V")
    ConstructorTest(T a, Runnable r) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, boolean[])", signature = "(TT;[Z)V")
    ConstructorTest(T a, boolean[] b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, byte[])", signature = "(TT;[B)V")
    ConstructorTest(T a, byte[] b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, char[])", signature = "(TT;[C)V")
    ConstructorTest(T a, char[] b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, short[])", signature = "(TT;[S)V")
    ConstructorTest(T a, short[] b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, int[])", signature = "(TT;[I)V")
    ConstructorTest(T a, int[] b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, long[])", signature = "(TT;[J)V")
    ConstructorTest(T a, long[] b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, float[])", signature = "(TT;[F)V")
    ConstructorTest(T a, float[] b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, double[])", signature = "(TT;[D)V")
    ConstructorTest(T a, double[] b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, java.lang.Runnable[])", signature = "(TT;[Ljava/lang/Runnable;)V")
    ConstructorTest(T a, Runnable[] r) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object[])", signature = "([TT;)V")
    ConstructorTest(T[] a) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Runnable[])",
            signature = "<T::Ljava/lang/Runnable;>([TT;)V")
    <T extends Runnable> ConstructorTest(T...a) {
    }

    @ExpectedSignature(descriptor = "<init>(java.util.Map)", signature = "(Ljava/util/Map<**>;)V")
    ConstructorTest(Map<?, ?> a) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object)", signature = "(TT;)V")
    ConstructorTest(T a) {
    }

    @ExpectedSignature(descriptor = "<init>(java.util.Set, java.util.Set)",
            signature = "<E::Ljava/util/Set<+TT;>;>(TE;TE;)V")
    <E extends Set<? extends T>> ConstructorTest(E a, E b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.ref.ReferenceQueue, java.lang.ref.ReferenceQueue)",
            signature = "<E:Ljava/lang/ref/ReferenceQueue<-TT;>;:Ljava/util/Map<-TT;+TT;>;>(TE;TE;)V")
    <E extends ReferenceQueue<? super T> & Map<? super T, ? extends T>> ConstructorTest(E a, E b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.util.List)", signature = "(Ljava/util/List<+TT;>;)V")
    ConstructorTest(List<? extends T> b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.util.Set)", signature = "(Ljava/util/Set<-TT;>;)V")
    ConstructorTest(Set<? super T> b) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Runnable)", signature = "<E::Ljava/lang/Runnable;>(TE;)V")
    <E extends Runnable> ConstructorTest(E a) {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.Object, java.lang.Object)", signature = "<E:TT;>(TT;TE;)V")
    <E extends T> ConstructorTest(T a, E b) {
    }

    // no Signature attribute
    ConstructorTest(boolean b) {
    }

    // no Signature attribute
    ConstructorTest(HashMap a) {
    }

    // no Signature attribute
    ConstructorTest(boolean[] b) {
    }

    // no Signature attribute
    ConstructorTest(HashMap[] a) {
    }
}
