/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary Unit test for formatter
 * @bug 4906370 4962433 4973103 4989961 5005818 5031150 4970931 4989491 5002937
 *      5005104 5007745 5061412 5055180 5066788 5088703 6317248 6318369 6320122
 *      6344623 6369500 6534606 6282094 6286592 6476425 5063507 6469160 6476168
 *      8059175 8204229
 *
 * @run shell/timeout=240 Basic.sh
 */

import static java.lang.System.out;

public class Basic {

    private static int fail = 0;
    private static int pass = 0;

    private static Throwable first;

    static void pass() {
        pass++;
    }

    static void fail(String fs, Class ex) {
        String s = "'" + fs + "': " + ex.getName() + " not thrown";
        if (first == null)
            setFirst(s);
        System.err.println("FAILED: " + s);
        fail++;
    }

    static void fail(String fs, String exp, String got) {
        String s = "'" + fs + "': Expected '" + exp + "', got '" + got + "'";
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

    static void ck(String fs, String exp, String got) {
        if (!exp.equals(got))
            fail(fs, exp, got);
        else
            pass();
    }

    public static void main(String[] args) {
        BasicBoolean.test();
        BasicBooleanObject.test();
        BasicByte.test();
        BasicByteObject.test();
        BasicChar.test();
        BasicCharObject.test();
        BasicShort.test();
        BasicShortObject.test();
        BasicInt.test();
        BasicIntObject.test();
        BasicLong.test();
        BasicLongObject.test();
        BasicBigInteger.test();
        BasicFloat.test();
        BasicFloatObject.test();
        BasicDouble.test();
        BasicDoubleObject.test();
        BasicBigDecimal.test();

        BasicDateTime.test();

        if (fail != 0)
            throw new RuntimeException((fail + pass) + " tests: "
                                       + fail + " failure(s), first", first);
        else
            out.println("all " + (fail + pass) + " tests passed");
    }
}
