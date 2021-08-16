/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6378821
 * @summary where available, bitCount() should use POPC on SPARC processors and AMD+10h
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.codegen.Test6378821::fcomp
 *      compiler.codegen.Test6378821
 */

package compiler.codegen;

public class Test6378821 {
    static final int[]  ia = new int[]  { 0x12345678 };
    static final long[] la = new long[] { 0x12345678abcdefL };

    public static void main(String [] args) {
        // Resolve the class and the method.
        Integer.bitCount(1);
        Long.bitCount(1);

        sub(ia[0]);
        sub(la[0]);
        sub(ia);
        sub(la);
    }

    static void check(int i, int expected, int result) {
        if (result != expected) {
            throw new InternalError("Wrong population count for " + i + ": " + result + " != " + expected);
        }
    }

    static void check(long l, int expected, int result) {
        if (result != expected) {
            throw new InternalError("Wrong population count for " + l + ": " + result + " != " + expected);
        }
    }

    static void sub(int i)     { check(i,     fint(i),  fcomp(i) ); }
    static void sub(int[] ia)  { check(ia[0], fint(ia), fcomp(ia)); }
    static void sub(long l)    { check(l,     fint(l),  fcomp(l) ); }
    static void sub(long[] la) { check(la[0], fint(la), fcomp(la)); }

    static int fint (int i)     { return Integer.bitCount(i); }
    static int fcomp(int i)     { return Integer.bitCount(i); }

    static int fint (int[] ia)  { return Integer.bitCount(ia[0]); }
    static int fcomp(int[] ia)  { return Integer.bitCount(ia[0]); }

    static int fint (long l)    { return Long.bitCount(l); }
    static int fcomp(long l)    { return Long.bitCount(l); }

    static int fint (long[] la) { return Long.bitCount(la[0]); }
    static int fcomp(long[] la) { return Long.bitCount(la[0]); }
}
