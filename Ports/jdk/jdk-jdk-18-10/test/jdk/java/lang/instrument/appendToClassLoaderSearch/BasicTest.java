/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * Basic unit test for Instrumentation appendToBootstrapClassLoaderSearch and
 * appendToSystemClassLoaderSearch methods:
 *
 * 1. Adds a jar file to the bootstrap class loader search; Loads a class known
 *    to be in the jar file and checks that it was loaded by the bootstrap class
 *    loader.
 *
 * 2. Adds a jar file to the system class loader search; Loads a class known
 *    to be in the jar file and checks that it was indeed loaded by the system
 *    class loader.
 */
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;
import java.io.IOException;

public class BasicTest {

    // count of failures
    static int failures = 0;

    // check that the given class is loaded by the given loader
    static void checkLoadedByLoader(String name, ClassLoader loader) {
        try {
            Class cl = Class.forName(name);
            ClassLoader actual = cl.getClassLoader();
            String loaderName = (actual == null) ? "boot class loader" : actual.toString();
            if (actual != loader) {
                System.out.println("FAIL: " + name + " incorrectly loaded by: " + loaderName);
                failures++;
            } else {
                System.out.println("PASS: " + name + " loaded by: " + loaderName);
            }
        } catch (Exception x) {
            System.out.println("FAIL: Unable to load " + name + ":" + x);
            failures++;
        }
    }

    public static void main(String args[]) throws IOException {
        JarFile bootclasses = new JarFile("BootSupport.jar");
        JarFile agentclasses = new JarFile("AgentSupport.jar");

        Instrumentation ins = Agent.getInstrumentation();

        // Test 1: Add BootSupport.jar to boot class path and check that
        // BootSupport is loaded by the bootstrap class loader
        ins.appendToBootstrapClassLoaderSearch(bootclasses);
        checkLoadedByLoader("BootSupport", null);

        // Test 2: Add AgentSupport.jar to the system class path and check that
        // AgentSupport is loaded by the system class loader.
        try {
            ins.appendToSystemClassLoaderSearch(agentclasses);
            checkLoadedByLoader("AgentSupport", ClassLoader.getSystemClassLoader());
        } catch (UnsupportedOperationException x) {
            System.out.println("System class loader does not support adding to class path");
        }

        // throw exception if a test failed
        if (failures > 0) {
            throw new RuntimeException(failures + " test(s) failed.");
        }
    }
}
