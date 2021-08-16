/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6215213
 * @summary Compiler JDK1.5 crashes with uses of generics
 * @author  Peter von der Ah\u00e9
 * @compile T6215213.java
 */

public class T6215213 {
    static class Box<T> {}
    static class Box1<T extends T6215213> {}
    static class Pair<T, S> {}
    static class Pair1<T extends T6215213, S> {}
    static class Triple<T, S, U> {}
    static class Triple1<T extends T6215213, S, U extends T6215213> {}
    static class Quad<T, S, U, V> {}
    static class Quad1<T extends T6215213, S, U extends T6215213, V> {}

    <T> Box<T> testBox(T t) { return null; }
    <T extends T6215213> Box1<T> testBox1(T t) { return null; }
    <T> Pair<T, T> testPair(T t) { return null; }
    <T extends T6215213> Pair1<T, T> testPair1(T t) { return null; }
    <T> Triple<T, T, T> testTriple(T t) { return null; }
    <T extends T6215213> Triple1<T, T, T> testTriple1(T t) { return null; }
    <T> Quad<T, T, T, T> testQuad(T t) { return null; }
    <T extends T6215213> Quad1<T, T, T, T> testQuad1(T t) { return null; }

    void testAll() {
        Box<?> box = testBox(null);
        Box1<?> box1 = testBox1(null);
        Pair<?, ?> pair = testPair(null);
        Pair1<?, ?> pair1 = testPair1(null);
        Triple<?, ?, ?> triple = testTriple(null);
        Triple1<?, ?, ?> triple1 = testTriple1(null);
        Quad<?, ?, ?, ?> quad = testQuad(null);
        Quad1<?, ?, ?, ?> quad1 = testQuad1(null);
    }
}
