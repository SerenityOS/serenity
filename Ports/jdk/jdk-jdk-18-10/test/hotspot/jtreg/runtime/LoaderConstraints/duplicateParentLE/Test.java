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

/**
 * @test
 * @bug 8199852
 * @summary Test exception messages of LinkageError with a parent class loader
 *          that is not known to the VM. A class loader loads
 *          twice the same class. Should trigger exception in
 *          SystemDictionary::check_constraints().
 * @compile ../common/Foo.java
 * @compile ../common/J.java
 * @compile ParentClassLoader.java
 *          PreemptingChildClassLoader.java
 * @run main/othervm Test
 */

public class Test {

    // Check that all names have external formatting ('.' and not '/' in package names).
    // Check for parent of class loader.
    // Break expectedErrorMessage into 2 parts due to the class loader name containing
    // the unique @<id> identity hash which cannot be compared against.

    // Check that all names have external formatting ('.' and not '/' in package names).
    // Check for name and parent of class loader. Type should be mentioned as 'class'.
    static String expectedErrorMessage_part1 = "loader 'DuplicateParentLE_Test_Loader' @";
    static String expectedErrorMessage_part2 = " attempted duplicate class definition for test.Foo. (test.Foo is in unnamed module of loader 'DuplicateParentLE_Test_Loader' @";
    static String expectedErrorMessage_part3 = ", parent loader ParentClassLoader @";


    // Test that the error message is correct when a loader constraint error is
    // detected during vtable creation.
    //
    // In this test, during vtable creation for class Task, method "Task.m()LFoo;"
    // overrides "J.m()LFoo;".  But, Task's class Foo and super type J's class Foo
    // are different.  So, a LinkageError exception should be thrown because the
    // loader constraint check will fail.
    public static void test(String loaderName,
                            String testType) throws Exception {
        String[] classNames = {testType};
        ClassLoader l = new PreemptingChildClassLoader(loaderName, classNames, false);
        l.loadClass(testType);
        try {
            l.loadClass(testType).newInstance();
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
        test("DuplicateParentLE_Test_Loader", "test.Foo");
    }
}
