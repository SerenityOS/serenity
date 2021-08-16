/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8251535
 * @summary Tests partial peeling at unsigned tests.
 * @library /test/lib /
 * @run main/othervm -Xcomp -XX:-TieredCompilation
 *                   -XX:CompileCommand=compileonly,compiler.loopopts.TestPartialPeelAtUnsignedTests::test*
 *                   compiler.loopopts.TestPartialPeelAtUnsignedTests
 */

package compiler.loopopts;

import jdk.test.lib.Asserts;

public class TestPartialPeelAtUnsignedTests {
    /*
        loop:
            i += 1000;
            if (i <u 10_000) {
                goto exit;
            }
            goto loop;
        exit:
            return i;

        C2 splits the unsigned loop exit check to have a
        signed exit test as cut point for partial peeling:

        loop:
            i += 1000;
            if (i < 10_000) {   <-- This exit condition is wrong!
                goto exit;
            }
            if (i <u 10_000) {
                goto exit;
            }
            goto loop;
        exit:
            return i;
    */
    static int test1(int i) {
        boolean cond = false;
        while (!cond) {
            i += 1000;
            // Converted to (i <u 10_000)
            cond = (0 <= i) && (i < 10_000);
        }
        return i;
    }

    /*
        Same as test1 but with a negative stride.

        loop:
            i -= 1000;
            if (i <u 10_000) {
                goto exit;
            }
            goto loop;
        exit:
            return i;

        Converted to:

        loop:
            i -= 1000;
            if (i >= 0) {   <-- This exit condition is wrong!
                goto exit;
            }
            if (i <u 10_000) {
                goto exit;
            }
            goto loop;
        exit:
            return i;
    */
    static int test2(int i) {
        boolean cond = false;
        while (!cond) {
            i -= 1000;
            cond = (0 <= i) && (i < 10_000);
        }
        return i;
    }

    /*
        Same as test1 but with inverted exit condition.

        loop:
            i += 1000;
            if (i <u 10_000) {
                goto loop;
            }
            goto exit;
        exit:
            return i;

        Converted to:

        loop:
            i += 1000;
            if (!(i < 10_000)) {   <-- Correct exit condition.
                goto exit;
            }
            if (i <u 10_000) {
                goto loop;
            }
            goto exit;
        exit:
            return i;
    */
    static int test3(int i) {
        boolean cond = true;
        while (cond) {
            i += 1000;
            cond = (0 <= i) && (i < 10_000);
        }
        return i;
    }

    /*
        Same as test2 but with inverted exit condition.

        loop:
            i -= 1000;
            if (i <u 10_000) {
                goto loop;
            }
            goto exit;
        exit:
            return i;

        Converted to:

        loop:
            i -= 1000;
            if (!(i >= 0)) {   <-- Correct exit condition.
                goto exit;
            }
            if (i <u 10_000) {
                goto loop;
            }
            goto exit;
        exit:
            return i;
    */
    static int test4(int i) {
        boolean cond = true;
        while (cond) {
            i -= 1000;
            cond = (0 <= i) && (i < 10_000);
        }
        return i;
    }

    public static void main(String[] args) {
        Asserts.assertEQ(test1(10_000), 704);
        Asserts.assertEQ(test2(-10_000), 9296);
        Asserts.assertEQ(test3(0), 10000);
        Asserts.assertEQ(test4(9999), -1);
    }
}
