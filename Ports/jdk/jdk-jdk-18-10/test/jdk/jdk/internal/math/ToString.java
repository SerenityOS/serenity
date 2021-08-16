/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/jdk.internal.math
 */

import jdk.internal.math.FloatingDecimal;

public class ToString {
    private static int fail = 0;

    private static Throwable first;

    public static void main(String argv[]) {
        test("10.0");
        test("1.0");
        test("0.1");
        test("0.01");
        test("0.001");
        test("1.0E-4");
        if (fail != 0)
            throw new RuntimeException(fail + " failure(s), first", first);
    }

    private static void test(String exp) {
        float c = Float.parseFloat(exp);
        String got = FloatingDecimal.toJavaFormatString(c);
        if (!got.equals(exp))
            fail("float '" + "': Expected '" + exp + "', got '" + got + "'");

        double d = Double.parseDouble(exp);
        got = FloatingDecimal.toJavaFormatString(d);
        if (!got.equals(exp))
            fail("double '" + "': Expected '" + exp + "', got '" + got + "'");
    }

    private static void fail(String s) {
        if (first == null)
            setFirst(s);
        System.err.println("FAILED: " + s);
        fail++;
    }

    private static void setFirst(String s) {
        try {
            throw new RuntimeException(s);
        } catch (RuntimeException x) {
            first = x;
        }
    }
}
