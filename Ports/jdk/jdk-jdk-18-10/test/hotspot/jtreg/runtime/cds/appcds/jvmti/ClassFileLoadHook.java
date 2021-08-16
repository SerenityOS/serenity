/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import sun.hotspot.WhiteBox;

class LoadMe {
    static String getValue() {
        return "beforeHook";
    }
    static String getOtherValue() {
        return "abc-beforeHook-xyz";
    }
}

public class ClassFileLoadHook {
    public enum TestCaseId {
        SHARING_OFF_CFLH_ON,   // test case to establish a baseline
        SHARING_ON_CFLH_OFF,
        SHARING_AUTO_CFLH_ON,
        SHARING_ON_CFLH_ON
    }

    public static void main(String args[]) throws Exception {
        TestCaseId testCase = TestCaseId.valueOf(args[0]);
        test1(testCase);
        test2(testCase);
    }

    // Test rewriting the classfile data using CFLH
    static void test1(TestCaseId testCase) {
        WhiteBox wb = WhiteBox.getWhiteBox();

        System.out.println("====== ClassFileLoadHook.main():testCase = " + testCase);
        System.out.println("getValue():" + LoadMe.getValue());
        System.out.println("getOtherValue():" + LoadMe.getOtherValue());

        switch (testCase) {
            case SHARING_OFF_CFLH_ON:
                assertTrue("after_Hook".equals(LoadMe.getValue()) &&
                           "abc-after_Hook-xyz".equals(LoadMe.getOtherValue()),
                           "Not sharing, this test should replace beforeHook " +
                           "with after_Hook");
            break;

            case SHARING_ON_CFLH_OFF:
                assertTrue(wb.isSharedClass(LoadMe.class),
                    "LoadMe should be shared, but is not");
                assertTrue("beforeHook".equals(LoadMe.getValue()) &&
                           "abc-beforeHook-xyz".equals(LoadMe.getOtherValue()),
                           "CFLH off, bug values are redefined");
            break;

            case SHARING_AUTO_CFLH_ON:
            case SHARING_ON_CFLH_ON:
                // LoadMe is rewritten on CFLH
                assertFalse(wb.isSharedClass(LoadMe.class),
                    "LoadMe should not be shared if CFLH has modified the class");
                assertFalse("beforeHook".equals(LoadMe.getValue()) &&
                           "abc-beforeHook-xyz".equals(LoadMe.getOtherValue()),
                           "Class contents should be changed if CFLH is enabled");
             break;

             default:
                 throw new RuntimeException("Invalid testcase");

        }
    }

    // Test the loading of classfile data for non-boot shared classes from jrt:/xxx.
    // See JDK-8221351.
    static void test2(TestCaseId testCase) throws Exception {
        WhiteBox wb = WhiteBox.getWhiteBox();
        Class c = Class.forName("java.sql.SQLException"); // defined by platform class loader.

        switch (testCase) {
            case SHARING_ON_CFLH_OFF:
            case SHARING_ON_CFLH_ON:
                assertTrue(wb.isSharedClass(c), "must be shared");
                break;
            case SHARING_AUTO_CFLH_ON:
                // With -Xshare:auto, the test continues to run with mapping failure.
                // In case of mapping failure, java/lang/Object and the app class
                // won't be loaded from the archive.
                Class objClass = Class.forName("java.lang.Object");
                if (wb.isSharedClass(objClass)) {
                    assertTrue(wb.isSharedClass(c), "must be shared");
                } else {
                    assertFalse(wb.isSharedClass(c), "must not be shared");
                }
                break;
            default:
                // this test is not applicable to -Xshare:off
                if (testCase != TestCaseId.SHARING_OFF_CFLH_ON) {
                    throw new RuntimeException("Invalid testcase");
                }
        }
    }

    private static void assertTrue(boolean expr, String msg) {
        if (!expr)
            throw new RuntimeException(msg);
    }

    private static void assertFalse(boolean expr, String msg) {
        if (expr)
            throw new RuntimeException(msg);
    }
}
