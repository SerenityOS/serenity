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
 */

/*
 * @test
 * @bug 8158297 8218939
 * @summary Constant pool utf8 entry for class name cannot have empty qualified name '//'
 * @compile p1/BadInterface1.jcod
 * @compile p1/BadInterface2.jcod
 * @compile UseBadInterface1.jcod
 * @compile UseBadInterface2.jcod
 * @run main/othervm -Xverify:all TestBadClassName
 */

public class TestBadClassName {
    public static void main(String args[]) throws Throwable {

        System.out.println("Regression test for bug 8042660");

        // Test class name with p1//BadInterface1
        String expected = "Illegal class name \"p1//BadInterface1\" in class file UseBadInterface1";
        try {
            Class newClass = Class.forName("UseBadInterface1");
            throw new RuntimeException("Expected ClassFormatError exception not thrown");
        } catch (java.lang.ClassFormatError e) {
            check(e, expected);
            System.out.println("Test UseBadInterface1 passed test case with illegal class name");
        }

        // Test class name with p1/BadInterface2/
        expected = "Illegal class name \"p1/BadInterface2/\" in class file UseBadInterface2";
        try {
            Class newClass = Class.forName("UseBadInterface2");
            throw new RuntimeException("Expected ClassFormatError exception not thrown");
        } catch (java.lang.ClassFormatError e) {
            check(e, expected);
            System.out.println("Test UseBadInterface2 passed test case with illegal class name");
        }
    }

    static void check(ClassFormatError c, String expected) {
        if (!c.getMessage().equals(expected)) {
            throw new RuntimeException("Wrong ClassFormatError - expected: \"" +
                                       expected + "\", got \"" +
                                       c.getMessage() + "\"");
        }
    }
}
