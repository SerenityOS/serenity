/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4240710
 * @summary RMIClassLoader.getClassLoader for a codebase should return the
 * same class loader that is used to load classes from that codebase (using
 * RMIClassLoader.loadClass).
 * @author Ann Wollrath
 *
 * @library ../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary Foo
 * @run main/othervm/policy=security.policy GetClassLoader
 */

import java.net.*;
import java.rmi.*;
import java.rmi.server.*;

public class GetClassLoader
{

    public static void main(String[] args) {

        System.err.println("\nTest for rfe 4240710\n");

        URL codebase1 = null;
        Class cl = null;
        ClassLoader loader = null;

        TestLibrary.suggestSecurityManager(null);

        try {
            codebase1 =
                TestLibrary.installClassInCodebase("Foo", "codebase1");
        } catch (MalformedURLException e) {
            TestLibrary.bomb(e);
        }


        /*
         * Force SecurityException for attempt to obtain a class loader
         * for a codebase that the test does not have permission to read from.
         */
        try {
            System.err.println(
                "getClassLoader for codebase that I can't read");
            loader = RMIClassLoader.getClassLoader("file:/foo");
            TestLibrary.bomb(
                "Failed: no SecurityException obtaining loader");
        } catch (MalformedURLException e) {
            System.err.println(
                "Failed: MalformedURLException getting loader");
            TestLibrary.bomb(e);
        } catch (SecurityException e) {
            System.err.println(
                "Passed: SecurityException obtaining loader");
        }

        /*
         * Verify that a specific class loaded by name via
         * RMIClassLoader.loadClass is the same as the class obtained by
         * loading that class by name through the loader obtained using
         * the RMIClassLoader.getClassLoader method.
         */
        System.err.println("load Foo from codebase1");
        try {
            cl = RMIClassLoader.loadClass(codebase1.toString(), "Foo");
        } catch (Exception e) {
            System.err.println(
                "Failed: exception loading class from codebase1");
            TestLibrary.bomb(e);
        }

        System.err.println(
            "load Foo using loader obtained using getClassLoader");
        try {
            loader = RMIClassLoader.getClassLoader(codebase1.toString());
        } catch (MalformedURLException e) {
            System.err.println(
                "Failed: MalformedURLException getting codebase1 loader");
            TestLibrary.bomb(e);
        }

        try {
            if (cl == loader.loadClass("Foo")) {
                System.err.println("Passed: Foo classes are equal");
            } else {
                TestLibrary.bomb("Failed: Foo classes are not equal");
            }
        } catch (Exception e) {
            System.err.println(
                "Failed: exception loading class from codebase1");
            TestLibrary.bomb(e);
        }

        /*
         * Force MalformedURLException by passing bogus URL to
         * getClassLoader.
         */
        try {
            loader = RMIClassLoader.getClassLoader("malformed:///URL");
            TestLibrary.bomb(
                "Failed: getClassLoader should throw MalformedURLException");
        } catch (MalformedURLException e) {
            System.err.println(
                "Passed: getClassLoader threw MalformedURLException");
        }
    }
}
