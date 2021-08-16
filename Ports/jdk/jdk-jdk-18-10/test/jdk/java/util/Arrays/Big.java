/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5045582
 * @summary arrays larger than 1<<30
 * @author Martin Buchholz
 */

// A proper regression test for 5045582 requires too much memory.
// If you have a really big machine, run like this:
// java -Xms25g -Xmx25g Big 30

import java.util.*;

public class Big {

    private static void realMain(String[] args) throws Throwable {
        final int shift = intArg(args, 0, 10); // "30" is real test
        final int tasks = intArg(args, 1, ~0); // all tasks
        final int n = (1<<shift) + 47;

        // To test byte arrays larger than 1<<30, you need 1600MB. Run like:
        // java -Xms1600m -Xmx1600m Big 30 1
        if ((tasks & 0x1) != 0) {
            System.out.println("byte[]");
            System.gc();
            byte[] a = new byte[n];
            a[0]   = (byte) -44;
            a[1]   = (byte) -43;
            a[n-2] = (byte) +43;
            a[n-1] = (byte) +44;
            for (int i : new int[] { 0, 1, n-2, n-1 })
                try { equal(i, Arrays.binarySearch(a, a[i])); }
                catch (Throwable t) { unexpected(t); }
            for (int i : new int[] { n-2, n-1 })
                try { equal(i, Arrays.binarySearch(a, n-5, n, a[i])); }
                catch (Throwable t) { unexpected(t); }

            a[n-19] = (byte) 45;
            try { Arrays.sort(a, n-29, n); }
            catch (Throwable t) { unexpected(t); }
            equal(a[n-1], (byte) 45);
            equal(a[n-2], (byte) 44);
            equal(a[n-3], (byte) 43);
            equal(a[n-4], (byte)  0);
        }

        // To test Object arrays larger than 1<<30, you need 13GB. Run like:
        // java -Xms13g -Xmx13g Big 30 2
        if ((tasks & 0x2) != 0) {
            System.out.println("Integer[]");
            System.gc();
            Integer[] a = new Integer[n];
            Integer ZERO = 0;
            Arrays.fill(a, ZERO);
            a[0]   =  -44;
            a[1]   =  -43;
            a[n-2] =  +43;
            a[n-1] =  +44;
            for (int i : new int[] { 0, 1, n-2, n-1 })
                try { equal(i, Arrays.binarySearch(a, a[i])); }
                catch (Throwable t) { unexpected(t); }
            for (int i : new int[] { n-2, n-1 })
                try { equal(i, Arrays.binarySearch(a, n-5, n, a[i])); }
                catch (Throwable t) { unexpected(t); }

            a[n-19] = 45;
            try { Arrays.sort(a, n-29, n); }
            catch (Throwable t) { unexpected(t); }
            equal(a[n-1],  45);
            equal(a[n-2],  44);
            equal(a[n-3],  43);
            equal(a[n-4],   0);
        }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
    static int intArg(String[] args, int i, int defaultValue) {
        return args.length > i ? Integer.parseInt(args[i]) : defaultValue;}
}
