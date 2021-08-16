/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

public class ArrayTest implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    byte b[] = { 0, 1};
    short s[] = { 0, 1, 2};
    char c[] = { 'Z', 'Y', 'X'};
    int i[] = { 0, 1, 2, 3, 4};
    long l[] = { 0, 1, 2, 3, 4, 5};
    boolean z[] = new boolean[4];
    float f[] = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f};
    double d[] = { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d, 7.0d};
    String string[] = { "ABC", "DEF", "GHI", "JKL"};
    PrimitivesTest prim[] = { new PrimitivesTest(), new PrimitivesTest() } ;

    transient int ti[] =  {99, 98, 97, 96};
    ArrayTest self = this;

    static  int si[] = {9, 8, 7, 6, 4} ;

    public ArrayTest() {
        z[0] = true;
        z[1] = false;
        z[2] = true;
        z[3] = false;
    }

    public boolean equals(ArrayTest other) {
        boolean ret = true;
        if (other == null) {
            System.err.println("\nother Array is " + other);
            return false;
        }
        if (!ArrayOpsTest.verify(i, other.i)) {
            System.err.println("\nUnpickling of int array failed");
            ret = false;
        }
        if (!ArrayOpsTest.verify(b, other.b)) {
            System.err.println("\nUnpickling of byte array failed");
            ret = false;
        }
        if (!ArrayOpsTest.verify(s, other.s)) {
            System.err.println("\nUnpickling of short array failed");
            ret = false;
        }
        if (!ArrayOpsTest.verify(c, other.c)) {
            System.err.println("\nUnpickling of char array failed");
            ret = false;
        }
        if (!ArrayOpsTest.verify(l, other.l)) {
            System.err.println("\nUnpickling of long array failed");
            ret = false;
        }
        if (!ArrayOpsTest.verify(f, other.f)) {
            System.err.println("\nUnpickling of float array failed");
            ret = false;
        }
        if (!ArrayOpsTest.verify(d, other.d)) {
            System.err.println("\nUnpickling of double array failed");
            ret = false;
        }
        if (!ArrayOpsTest.verify(z, other.z)) {
            System.err.println("\nUnpickling of boolean array failed");
            ret = false;
        }
        if (!ArrayOpsTest.verify(string, other.string)) {
            System.err.println("\nUnpickling of String array failed");
            ret = false;
        }
        if (!ArrayOpsTest.verify(prim, other.prim)) {
            System.err.println("\nUnpickling of Primitives array failed");
            ret = false;
        }
        return ret;
    }
}
