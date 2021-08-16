/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8186092 8199852
 * @compile ../common/Foo.java
 *          ../common/J.java
 *          I.java
 *          ../common/C.jasm
 *          Task.java
 *          ../common/PreemptingClassLoader.java
 * @run main/othervm Test
 */

public class Test {

    // Break expected error messages into 3 parts since the loader name includes its identity
    // hash which is unique and can't be compared against.
    static String expectedErrorMessage1_part1 = "loader constraint violation for class test.Task: when " +
                                                "selecting overriding method 'test.Foo test.Task.m()' the " +
                                                "class loader PreemptingClassLoader @";
    static String expectedErrorMessage1_part2 = " of the selected method's type test.Task, and the class " +
                                                "loader 'app' for its super type test.J have different Class objects " +
                                                "for the type test.Foo used in the signature (test.Task is in unnamed " +
                                                "module of loader PreemptingClassLoader @";
    static String expectedErrorMessage1_part3 = ", parent loader 'app'; test.J is in unnamed module of loader 'app')";

    static String expectedErrorMessage2_part1 = "loader constraint violation for class test.Task: when " +
                                                "selecting overriding method 'test.Foo test.Task.m()' the " +
                                                "class loader 'VtableLdrCnstrnt_Test_Loader' @";
    static String expectedErrorMessage2_part2 = " of the selected method's type test.Task, and the class " +
                                                "loader 'app' for its super type test.J have different Class objects " +
                                                "for the type test.Foo used in the signature (test.Task is in unnamed " +
                                                "module of loader 'VtableLdrCnstrnt_Test_Loader' @";
    static String expectedErrorMessage2_part3 = ", parent loader 'app'; test.J is in unnamed module of loader 'app')";

    // Test that the error message is correct when a loader constraint error is
    // detected during vtable creation.
    //
    // In this test, during vtable creation for class Task, method "Task.m()LFoo;"
    // overrides "J.m()LFoo;".  But, Task's class Foo and super type J's class Foo
    // are different.  So, a LinkageError exception should be thrown because the
    // loader constraint check will fail.
    public static void test(String loaderName,
                            String expectedErrorMessage_part1,
                            String expectedErrorMessage_part2,
                            String expectedErrorMessage_part3) throws Exception {
        Class<?> c = test.Foo.class; // Forces standard class loader to load Foo.
        String[] classNames = {"test.Task", "test.Foo", "test.I"};
        ClassLoader l = new PreemptingClassLoader(loaderName, classNames);
        l.loadClass("test.Foo");
        try {
            l.loadClass("test.Task").newInstance();
            throw new RuntimeException("Expected LinkageError exception not thrown");
        } catch (LinkageError e) {
            String errorMsg = e.getMessage();
            if (!errorMsg.contains(expectedErrorMessage_part1) ||
                !errorMsg.contains(expectedErrorMessage_part2) ||
                !errorMsg.contains(expectedErrorMessage_part3)) {
                System.out.println("Expected: " + expectedErrorMessage_part1 + "<id>" + expectedErrorMessage_part2 + "\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong LinkageError exception thrown: " + errorMsg);
            }
            System.out.println("Passed with message: " + errorMsg);
        }
    }

    public static void main(String args[]) throws Exception {
        test(null, expectedErrorMessage1_part1,
             expectedErrorMessage1_part2, expectedErrorMessage1_part3);
        test("VtableLdrCnstrnt_Test_Loader", expectedErrorMessage2_part1,
             expectedErrorMessage2_part2, expectedErrorMessage2_part3);
    }
}
