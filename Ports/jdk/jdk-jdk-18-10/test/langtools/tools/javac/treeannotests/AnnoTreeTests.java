/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @modules jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build DA TA Test TestProcessor
 * @compile -XDaccessInternalAPI -proc:only -processor TestProcessor AnnoTreeTests.java
 */

@Test(4)
class AnnoTreeTests {
    // primitive types
    // @TA("int") int i1 = 0; // TODO: Only visible via ClassFile
    long i2 = (@TA("long") long) 0;

    // simple array types
    // @DA("short") short[] a1; // TODO: Only visible via ClassFile
    byte @TA("byte[]") [] a2;
    float[] a3 = (@TA("float") float[]) null;
    double[] a4 = (double @TA("double[]") []) null;

    // multi-dimensional array types
    // (still to come)
}
