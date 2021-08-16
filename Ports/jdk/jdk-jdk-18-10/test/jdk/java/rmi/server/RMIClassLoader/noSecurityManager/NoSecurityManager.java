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
 * @bug 4140511
 * @summary RMIClassLoader's loadClass() methods, when there is no security
 * manager installed, should not create or use RMI class loader instances for
 * the requested codebases, but they should still succeed to load classes
 * that can be found by delegation to parent class loader that would have
 * been used for the RMI class loader instance.
 * @author Peter Jones
 *
 * @build Dummy LocalDummy
 * @run main/othervm/timeout=120 NoSecurityManager
 */

import java.io.*;
import java.net.*;
import java.security.*;
import java.rmi.server.RMIClassLoader;

/**
 * NoSecurityManager verifies the behavior of the RMIClassLoader.loadClass()
 * methods when there is no security manager installed in JDK1.2 as prescribed
 * by RFE 4140511.
 *
 * First, a class installed in a codebase (and not in the test's class path)
 * is loaded using RMIClassLoader.loadClass(); this should fail with a
 * ClassNotFoundException (it failed with a SecurityException in JDK 1.1).
 *
 * Second, a class installed in the test's class path is loaded using
 * RMIClassLoader.loadClass(), which should succeed (again, this used
 * to fail with a SecurityException in JDK 1.1).
 */
public class NoSecurityManager {

    /** name of target class to attempt to load */
    private static final String className = "Dummy";

    /** name of file containing definition for target class */
    private static final String classFileName = className + ".class";

    public static void main(String[] args) throws Exception {
        /*
         * Specify the file to contain the class definition.
         * Make sure that there is a "classes" subdirectory underneath
         * the working directory to be used as the codebase for the
         * class definition to be located.
         */
        File dstDir = new File(System.getProperty("user.dir"), "codebase");
        if (!dstDir.exists()) {
            if (!dstDir.mkdir()) {
                throw new RuntimeException(
                    "could not create codebase directory");
            }
        }
        File dstFile = new File(dstDir, classFileName);

        /*
         * Specify where we will copy the class definition from, if
         * necessary.  After the test is built, the class file can be
         * found in the "test.classes" directory.
         */
        File srcDir = new File(System.getProperty("test.classes", "."));
        File srcFile = new File(srcDir, classFileName);

        /*
         * If the class definition is not already located at the codebase,
         * copy it there from the test build area.
         */
        if (!dstFile.exists()) {
            if (!srcFile.exists()) {
                throw new RuntimeException(
                    "could not find class file to install in codebase " +
                    "(try rebuilding the test)");
            }
            if (!srcFile.renameTo(dstFile)) {
                throw new RuntimeException(
                    "could not install class file in codebase");
            }
        }

        /*
         * After the class definition is successfully installed at the
         * codebase, delete it from the test's CLASSPATH, so that it will
         * not be found there first before the codebase is searched.
         */
        if (srcFile.exists()) {
            if (!srcFile.delete()) {
                throw new RuntimeException(
                    "could not delete duplicate class file in CLASSPATH");
            }
        }

        /*
         * Obtain the URL for the codebase.
         */
        URL codebaseURL = new URL("file", "",
            dstDir.getAbsolutePath().replace(File.separatorChar, '/') + "/");

        /*
         * No security manager has been set: verify that we cannot load
         * a class from a specified codebase (that is not in the class path).
         */
        try {
            RMIClassLoader.loadClass(codebaseURL, className);
            throw new RuntimeException(
                "TEST FAILED: class loaded successfully from codebase");
        } catch (ClassNotFoundException e) {
            System.err.println(e.toString());
        }

        /*
         * No security manager has been set: verify that we can still
         * load a class available in the context class loader (class path).
         */
        RMIClassLoader.loadClass(codebaseURL, "LocalDummy");
        System.err.println("TEST PASSED: local class loaded successfully");

        /*
         * Verify that getClassLoader returns context class loader
         * if no security manager is set.
         */
        System.err.println("/nTest getClassLoader with no security manager set");
        ClassLoader loader = RMIClassLoader.getClassLoader("http://codebase");
        if (loader == Thread.currentThread().getContextClassLoader()) {
            System.err.println("TEST PASSED: returned context class loader");
        } else {
            throw new RuntimeException(
                "TEST FAILED: returned RMI-created class loader");
        }
    }
}
