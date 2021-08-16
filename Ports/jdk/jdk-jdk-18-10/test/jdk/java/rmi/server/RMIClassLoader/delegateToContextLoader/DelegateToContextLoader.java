/*
 * Copyright (c) 1998, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4112682
 * @summary RMIClassLoader's loadClass() method that takes explicit URLs
 * should load classes from a class loader that delegates to the current
 * thread's context class loader (not just the base class loader always).
 * @author Peter Jones
 *
 * @library ../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary Dummy
 * @run main/othervm/policy=security.policy/timeout=120 DelegateToContextLoader
 */

import java.io.*;
import java.net.*;
import java.security.*;
import java.rmi.RMISecurityManager;
import java.rmi.server.RMIClassLoader;

public class DelegateToContextLoader {

    /** name of target class to attempt to load */
    private static final String className = "Dummy";

    /** name of file containing definition for target class */
    private static final String classFileName = className + ".class";

    public static void main(String[] args) throws Exception {

        /*
         * Set a security manager so that RMI class loading will not
         * be disabled.
         */
        TestLibrary.suggestSecurityManager("java.rmi.RMISecurityManager");


        URL codebaseURL = TestLibrary.
            installClassInCodebase(className, "codebase");

         /* Create a URLClassLoader to load from the codebase and set it
          * as this thread's context class loader.  We do not use the
          * URLClassLoader.newInstance() method so that the test will
          * compile with more early versions of the JDK.
          *
          * We can get away with creating a class loader like this
          * because there is no security manager set yet.
          */
        ClassLoader codebaseLoader =
            new URLClassLoader(new URL[] { codebaseURL } );
        Thread.currentThread().setContextClassLoader(codebaseLoader);

        File srcDir = new File(TestLibrary.getProperty("test.classes", "."));

        URL dummyURL = new URL("file", "",
            srcDir.getAbsolutePath().replace(File.separatorChar, '/') +
            "/x-files/");

        try {
            /*
             * Attempt to load the target class from the dummy URL;
             * it should be found in the context class loader.
             */
            Class cl = RMIClassLoader.loadClass(dummyURL, className);
            System.err.println("TEST PASSED: loaded class: " + cl);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "TEST FAILED: target class in context class loader " +
                "not found using RMIClassLoader");
        }
    }
}
