/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8059299
 * @summary assert(adr_type != NULL) failed: expecting TypeKlassPtr
 *
 * @run main/othervm -Xbatch compiler.exceptions.CatchInlineExceptions
 */

package compiler.exceptions;

public class CatchInlineExceptions {
    static class Exception1 extends Exception {};
    static class Exception2 extends Exception {};
    private static int counter0;
    private static int counter1;
    private static int counter2;
    private static int counter;

    static void foo(int i) throws Exception {
        if ((i & 1023) == 2) {
            counter0++;
            throw new Exception2();
        }
    }

    static void test(int i) throws Exception {
        try {
           foo(i);
        }
        catch (Exception e) {
            if (e instanceof Exception1) {
                counter1++;
            } else if (e instanceof Exception2) {
                counter2++;
            }
            counter++;
            throw e;
        }
    }

    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < 15000; i++) {
            try {
                test(i);
            } catch (Exception e) {
                // expected
            }
        }
        if (counter1 != 0) {
            throw new RuntimeException("Failed: counter1(" + counter1  + ") != 0");
        }
        if (counter2 != counter0) {
            throw new RuntimeException("Failed: counter2(" + counter2  + ") != counter0(" + counter0  + ")");
        }
        if (counter2 != counter) {
            throw new RuntimeException("Failed: counter2(" + counter2  + ") != counter(" + counter  + ")");
        }
        System.out.println("TEST PASSED");
    }
}
