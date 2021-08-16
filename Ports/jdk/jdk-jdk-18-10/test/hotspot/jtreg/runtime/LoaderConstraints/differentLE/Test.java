/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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
 * @summary Test exception messages of LinkageError. Two class loaders load
 *          two different versions of a class. Should trigger exception in
 *          SystemDictionary::check_constraints().
 * @library /test/lib
 * @compile D_ambgs.jasm
 * @run driver jdk.test.lib.helpers.ClassFileInstaller test.D_ambgs
 * @compile  ../common/PreemptingClassLoader.java
 *           test/D_ambgs.java Test.java test/B.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller test.B
 * @run main/othervm Test
 */

import test.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Test {

    //  Force LinkageError.
    //
    //  Derived from test runtime/6626217.
    //
    //  Uses the specialized class loader PreemptingClassLoader.
    //  PreemptingClassLoader only loads files with names passed to it in its
    //  constructor. If it does not find it, it delegates to the super class loader.
    //
    //  A    // interface
    //  |
    //  B    // Compiled to the current working directory so that it is found by our
    //       // special class loader. B uses D, so that loading B triggers loading D.
    //
    //  C    // An abstract class.
    //  |
    //  D    // Class with two different implementations D1 and D2. D2 is
    //       // compiled to the current working directory so that it is found by our
    //       // special class loader.
    //
    // First, the bootstrap loader will load D1. It already has loaded interface A.
    // Then, the second class loader PreemptingClassLoader will load B. Recursive,
    // it tries to load interface A. As it does not find it (there is no A.impl2),
    // it asks the super classloader for A.
    // Then it loads the D2 variant of D from the current working directory and it's
    // superclass C. This fails as D1 is already loaded with the same superclass.

    // Break the expectedErrorMessage into 2 pieces since the loader name will include
    // its identity hash and can not be compared against.
    static String expectedErrorMessage_part1 = "loader constraint violation: loader PreemptingClassLoader @";
    static String expectedErrorMessage_part2 = " wants to load class test.D_ambgs. A different class " +
                                               "with the same name was previously loaded by 'app'. " +
                                               "(test.D_ambgs is in unnamed module of loader 'app')";
    public static void test_access() throws Exception {
        try {
            // Make a Class 'D_ambgs' under the default loader.
            // This uses the implementation from the .java file.
            C c_1 = new D_ambgs();

            // Some classes under a new Loader, loader2, including, indirectly,
            // another version of 'D_ambgs'
            String[] classNames = {"test.B", "test.D_ambgs"};

            ClassLoader loader2 = new PreemptingClassLoader(null, classNames, false);
            Class       class2  = loader2.loadClass("test.B");
            A           iface   = (A)class2.newInstance();

            // Call D1.make() loaded by bootstrap loader with B loaded by Loader2.
            D_ambgs[] x2 = c_1.make(iface);

            throw new RuntimeException("Expected LinkageError was not thrown.");
        } catch (LinkageError jle) {
            String errorMsg = jle.getMessage();
            if (!errorMsg.contains(expectedErrorMessage_part1) ||
                !errorMsg.contains(expectedErrorMessage_part2)) {
                System.out.println("Expected: " + expectedErrorMessage_part1 + "<id>" + expectedErrorMessage_part2 + "\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of LinkageError.");
            } else {
                System.out.println("Passed with message: " + errorMsg);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        test_access();
    }
}
