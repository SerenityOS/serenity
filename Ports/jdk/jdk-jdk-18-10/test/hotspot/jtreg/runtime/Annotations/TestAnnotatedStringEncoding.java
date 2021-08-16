/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.*;
import java.lang.reflect.*;

/*
 * @test
 * @bug 8054307
 * @summary Tests the correct encoding of latin1/UTF16 Strings used in annotations.
 */
public class TestAnnotatedStringEncoding {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Test {
        String str();
        int index();
    }

    public static void main(String[] args) throws Exception {
        new TestAnnotatedStringEncoding().run();
    }

    public void run() {
        // Iterate over annotated methods and retrieve the string
        for (Method m : this.getClass().getMethods()) {
            if (m.isAnnotationPresent(Test.class)) {
                // Check if string equals expected value
                Test test = m.getAnnotation(Test.class);
                String str = test.str();
                int index = test.index();
                if (!str.equals(strValue[index])) {
                    throw new RuntimeException(m.getName() + " failed: \"" + str + "\" (0x" + Integer.toHexString(str.charAt(0)) +
                            ") does not equal \"" + strValue[index] + "\" (0x" + Integer.toHexString(strValue[index].charAt(0)) + ") .");
                }
            }
        }
        System.out.println("Test passed.");
    }

    public static String[] strValue = {
        "\u0000", "\u0020", "\u0021", "\u0080",
        "\u00FF", "\u0100", "\u017F", "\u01FF",
        "\u07FF", "\u0800", "\uC280", "\uC2BF",
        "\uC380", "\uC3BF", "\uC5BF", "\uFFFF",
        "\u10000", "\u1FFFFF", "\u200000",
        "\u3FFFFFF", "\u4000000", "\u7FFFFFFF",
        "ab\uff23\uff24ef\uff27", "\uff21\uff22cd\uff25g", "\u00FF\u00FF\u00FF", "\u00A1\u00A1\u00A1\u00A1", ""};

    @Test(str = "\u0000", index = 0)
    public static void check0() { }

    @Test(str = "\u0020", index = 1)
    public static void check1() { }

    @Test(str = "\u0021", index = 2)
    public static void check2() { }

    @Test(str = "\u0080", index = 3)
    public static void check3() { }

    @Test(str = "\u00FF", index = 4)
    public static void check4() { }

    @Test(str = "\u0100", index = 5)
    public static void check5() { }

    @Test(str = "\u017F", index = 6)
    public static void check6() { }

    @Test(str = "\u01FF", index = 7)
    public static void check7() { }

    @Test(str = "\u07FF", index = 8)
    public static void check8() { }

    @Test(str = "\u0800", index = 9)
    public static void check9() { }

    @Test(str = "\uC280", index = 10)
    public static void check10() { }

    @Test(str = "\uC2BF", index = 11)
    public static void check11() { }

    @Test(str = "\uC380", index = 12)
    public static void check12() { }

    @Test(str = "\uC3BF", index = 13)
    public static void check13() { }

    @Test(str = "\uC5BF", index = 14)
    public static void check14() { }

    @Test(str = "\uFFFF", index = 15)
    public static void check15() { }

    @Test(str = "\u10000", index = 16)
    public static void check16() { }

    @Test(str = "\u1FFFFF", index = 17)
    public static void check17() { }

    @Test(str = "\u200000", index = 18)
    public static void check18() { }

    @Test(str = "\u3FFFFFF", index = 19)
    public static void check19() { }

    @Test(str = "\u4000000", index = 20)
    public static void check20() { }

    @Test(str = "\u7FFFFFFF", index = 21)
    public static void check21() { }

    @Test(str = "ab\uff23\uff24ef\uff27", index = 22)
    public static void check22() { }

    @Test(str = "\uff21\uff22cd\uff25g", index = 23)
    public static void check23() { }

    @Test(str = "\u00FF\u00FF\u00FF", index = 24)
    public static void check24() { }

    @Test(str = "\u00A1\u00A1\u00A1\u00A1", index = 25)
    public static void check25() { }

    @Test(str = "", index = 26)
    public static void check26() { }
}
