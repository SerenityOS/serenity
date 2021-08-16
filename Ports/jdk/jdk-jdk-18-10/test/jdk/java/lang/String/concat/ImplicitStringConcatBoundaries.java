/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test the boundary values for concatenation arguments.
 *
 * @compile ImplicitStringConcatBoundaries.java
 * @run main/othervm -Xverify:all ImplicitStringConcatBoundaries
 *
 * @compile -XDstringConcat=inline ImplicitStringConcatBoundaries.java
 * @run main/othervm -Xverify:all ImplicitStringConcatBoundaries
 *
 * @compile -XDstringConcat=indy ImplicitStringConcatBoundaries.java
 * @run main/othervm -Xverify:all ImplicitStringConcatBoundaries
 *
 * @compile -XDstringConcat=indyWithConstants ImplicitStringConcatBoundaries.java
 * @run main/othervm -Xverify:all ImplicitStringConcatBoundaries
 */

public class ImplicitStringConcatBoundaries {

    public static final boolean BOOL_TRUE_1         = true;
    public static       boolean BOOL_TRUE_2         = true;
    public static final boolean BOOL_FALSE_1        = false;
    public static       boolean BOOL_FALSE_2        = false;

    public static final byte    BYTE_MIN_1          = Byte.MIN_VALUE;
    public static       byte    BYTE_MIN_2          = Byte.MIN_VALUE;
    public static final byte    BYTE_MAX_1          = Byte.MAX_VALUE;
    public static       byte    BYTE_MAX_2          = Byte.MAX_VALUE;

    public static final short   SHORT_MIN_1         = Short.MIN_VALUE;
    public static       short   SHORT_MIN_2         = Short.MIN_VALUE;
    public static final short   SHORT_MAX_1         = Short.MAX_VALUE;
    public static       short   SHORT_MAX_2         = Short.MAX_VALUE;

    public static final char    CHAR_MIN_1          = Character.MIN_VALUE;
    public static       char    CHAR_MIN_2          = Character.MIN_VALUE;
    public static final char    CHAR_MAX_1          = Character.MAX_VALUE;
    public static       char    CHAR_MAX_2          = Character.MAX_VALUE;

    public static final int     INT_MIN_1           = Integer.MIN_VALUE;
    public static       int     INT_MIN_2           = Integer.MIN_VALUE;
    public static final int     INT_MAX_1           = Integer.MAX_VALUE;
    public static       int     INT_MAX_2           = Integer.MAX_VALUE;

    public static final float   FLOAT_MIN_EXP_1     = Float.MIN_EXPONENT;
    public static       float   FLOAT_MIN_EXP_2     = Float.MIN_EXPONENT;
    public static final float   FLOAT_MIN_NORM_1    = Float.MIN_NORMAL;
    public static       float   FLOAT_MIN_NORM_2    = Float.MIN_NORMAL;
    public static final float   FLOAT_MIN_1         = Float.MIN_VALUE;
    public static       float   FLOAT_MIN_2         = Float.MIN_VALUE;
    public static final float   FLOAT_MAX_1         = Float.MAX_VALUE;
    public static       float   FLOAT_MAX_2         = Float.MAX_VALUE;

    public static final long    LONG_MIN_1          = Long.MIN_VALUE;
    public static       long    LONG_MIN_2          = Long.MIN_VALUE;
    public static final long    LONG_MAX_1          = Long.MAX_VALUE;
    public static       long    LONG_MAX_2          = Long.MAX_VALUE;

    public static final double  DOUBLE_MIN_EXP_1    = Double.MIN_EXPONENT;
    public static       double  DOUBLE_MIN_EXP_2    = Double.MIN_EXPONENT;
    public static final double  DOUBLE_MIN_NORM_1   = Double.MIN_NORMAL;
    public static       double  DOUBLE_MIN_NORM_2   = Double.MIN_NORMAL;
    public static final double  DOUBLE_MIN_1        = Double.MIN_VALUE;
    public static       double  DOUBLE_MIN_2        = Double.MIN_VALUE;
    public static final double  DOUBLE_MAX_1        = Double.MAX_VALUE;
    public static       double  DOUBLE_MAX_2        = Double.MAX_VALUE;

    public static void main(String[] args) throws Exception {
        test("foofalse",                    "foo" + BOOL_FALSE_1);
        test("foofalse",                    "foo" + BOOL_FALSE_2);
        test("footrue",                     "foo" + BOOL_TRUE_1);
        test("footrue",                     "foo" + BOOL_TRUE_2);

        test("foo127",                      "foo" + BYTE_MAX_1);
        test("foo127",                      "foo" + BYTE_MAX_2);
        test("foo-128",                     "foo" + BYTE_MIN_1);
        test("foo-128",                     "foo" + BYTE_MIN_2);

        test("foo32767",                    "foo" + SHORT_MAX_1);
        test("foo32767",                    "foo" + SHORT_MAX_2);
        test("foo-32768",                   "foo" + SHORT_MIN_1);
        test("foo-32768",                   "foo" + SHORT_MIN_2);

        test("foo\u0000",                   "foo" + CHAR_MIN_1);
        test("foo\u0000",                   "foo" + CHAR_MIN_2);
        test("foo\uFFFF",                   "foo" + CHAR_MAX_1);
        test("foo\uFFFF",                   "foo" + CHAR_MAX_2);

        test("foo2147483647",               "foo" + INT_MAX_1);
        test("foo2147483647",               "foo" + INT_MAX_2);
        test("foo-2147483648",              "foo" + INT_MIN_1);
        test("foo-2147483648",              "foo" + INT_MIN_2);

        test("foo1.17549435E-38",           "foo" + FLOAT_MIN_NORM_1);
        test("foo1.17549435E-38",           "foo" + FLOAT_MIN_NORM_2);
        test("foo-126.0",                   "foo" + FLOAT_MIN_EXP_1);
        test("foo-126.0",                   "foo" + FLOAT_MIN_EXP_2);
        test("foo1.4E-45",                  "foo" + FLOAT_MIN_1);
        test("foo1.4E-45",                  "foo" + FLOAT_MIN_2);
        test("foo3.4028235E38",             "foo" + FLOAT_MAX_1);
        test("foo3.4028235E38",             "foo" + FLOAT_MAX_2);

        test("foo-9223372036854775808",     "foo" + LONG_MIN_1);
        test("foo-9223372036854775808",     "foo" + LONG_MIN_2);
        test("foo9223372036854775807",      "foo" + LONG_MAX_1);
        test("foo9223372036854775807",      "foo" + LONG_MAX_2);

        test("foo2.2250738585072014E-308",  "foo" + DOUBLE_MIN_NORM_1);
        test("foo2.2250738585072014E-308",  "foo" + DOUBLE_MIN_NORM_2);
        test("foo-1022.0",                  "foo" + DOUBLE_MIN_EXP_1);
        test("foo-1022.0",                  "foo" + DOUBLE_MIN_EXP_2);
        test("foo4.9E-324",                 "foo" + DOUBLE_MIN_1);
        test("foo4.9E-324",                 "foo" + DOUBLE_MIN_2);
        test("foo1.7976931348623157E308",   "foo" + DOUBLE_MAX_1);
        test("foo1.7976931348623157E308",   "foo" + DOUBLE_MAX_2);
    }

    public static void test(String expected, String actual) {
       if (!expected.equals(actual)) {
           StringBuilder sb = new StringBuilder();
           sb.append("Expected = ");
           sb.append(expected);
           sb.append(", actual = ");
           sb.append(actual);
           throw new IllegalStateException(sb.toString());
       }
    }
}
