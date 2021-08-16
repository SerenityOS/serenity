/*
 * Copyright (c) 1997, 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4030173 4071548
 * @summary Verify correct conversion of non-string arguments in string concatenation.
 * @author maddox
 *
 * @compile StringConversion.java
 * @run main StringConversion
 */

class FooBar {
    public String toString(){
        return null;
    }
}

public class StringConversion {

    static void check(int testid, String have, String expect)
                throws Exception {
        if ((have == null && have != expect) ||
                (have != null && !have.equals(expect))) {
            String msg =
                "TEST " + testid + ": HAVE \"" +
                have + "\" EXPECT \"" + expect + "\"";
            System.out.println("StringConversion: " + msg);
            throw new Exception(msg);
        }
    }

    public static void main(String[] args) throws Exception {

        String s;
        String n = null;
        Object o = null;
        FooBar m = new FooBar();

        // Null reference must be converted to "null"

        // Conversion will be done by 'StringBuffer.append'.
        s = "foo" + n;
        check(11, s, "foonull");
        s = n + "bar";
        check(12, s, "nullbar");
        s = "foo" + o;
        check(13, s, "foonull");
        s = o + "bar";
        check(14, s, "nullbar");

        // Conversion will be done by 'String.valueOf'.
        s = "" + n;
        check(21, s, "null");
        s = n + "";
        check(22, s, "null");
        s = "" + o;
        check(23, s, "null");
        s = o + "";
        check(24, s, "null");

        // Null 'toString' result must be converted to "null"

        // Conversion will be done by 'StringBuffer.append'.
        s = "foo" + m;
        check(31, s, "foonull");
        s = m + "bar";
        check(32, s, "nullbar");

        // Conversion will be done by 'String.valueOf'.
        s = "" + m;
        check(43, s, "null");
        s = m + "";
        check(44, s, "null");

        // A character array must be converted as if by
        // 'toString', i.e., it is treated as an 'Object'.

        s = "polymorph";
        char[] ca = {'i', 's', 'm'};

        check(51, s + ca, s + ca.toString());
        check(52, ca + s, ca.toString() + s);

        System.out.println("OK");
    }
}
