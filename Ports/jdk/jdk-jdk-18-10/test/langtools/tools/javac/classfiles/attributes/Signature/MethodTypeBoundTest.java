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
 * @summary Checks Signature attribute for type bounds.
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @build MethodTypeBoundTest Driver ExpectedSignature ExpectedSignatureContainer
 * @run main Driver MethodTypeBoundTest
 */

import java.util.*;

@ExpectedSignature(descriptor = "MethodTypeBoundTest", signature = "<T:Ljava/lang/Object;>Ljava/lang/Object;")
public class MethodTypeBoundTest<T> {

    @ExpectedSignature(descriptor = "method1(java.lang.String)",
            signature = "<E:Ljava/lang/String;:Ljava/lang/Runnable;:Ljava/util/Collection<+TT;>;>(TE;)TE;")
    <E extends String & Runnable & Collection<? extends T>> E method1(E a) {
        return a;
    }

    @ExpectedSignature(descriptor = "method2(java.lang.Runnable)",
            signature = "<E::Ljava/lang/Runnable;:Ljava/util/Collection<-TT;>;>(TE;)TE;")
    <E extends Runnable & Collection<? super T>> E method2(E a) {
        return a;
    }

    @ExpectedSignature(descriptor = "method3(java.util.ArrayList)",
            signature = "<E:Ljava/util/ArrayList<+TT;>;>(TE;)TE;")
    <E extends ArrayList<? extends T>> E method3(E a) {
        return a;
    }

    @ExpectedSignature(descriptor = "method4(java.util.LinkedList)",
            signature = "<E:Ljava/util/LinkedList<TE;>;:Ljava/util/List<TE;>;>(TE;)TE;")
    <E extends LinkedList<E> & List<E>> E method4(E a) {
        return a;
    }

    @ExpectedSignature(descriptor = "method5(java.util.Iterator)",
            signature = "<E:Ljava/util/LinkedList<TE;>;:Ljava/util/List<TE;>;" +
                    "U::Ljava/util/Iterator<-LMethodTypeBoundTest<TT;>.InnerClass<TE;>;>;>(TU;)TE;")
    <E extends LinkedList<E> & List<E>, U extends Iterator<? super InnerClass<E>>> E method5(U a) {
        return null;
    }

    @ExpectedSignature(descriptor = "method6()",
            signature = "<E:Ljava/util/LinkedList<TT;>;U:TE;>()V")
    <E extends LinkedList<T>, U extends E> void method6() {
    }

    @ExpectedSignature(descriptor = "MethodTypeBoundTest$InnerClass",
            signature = "<T:Ljava/lang/Object;>Ljava/lang/Object;")
    class InnerClass<T> {
    }
}
