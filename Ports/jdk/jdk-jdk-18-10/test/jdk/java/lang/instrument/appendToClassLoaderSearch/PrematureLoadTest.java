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
 * 1. Attempt to load a class that does not exist.
 *
 * 2. Add a jar file to the class path with the missing class.
 *
 * 3. Attempt to load the class again - CNF should be thrown as the JVMS requires that
 *    the error from the first resolution attempt should be thrown.
 */
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;
import java.io.IOException;

public class PrematureLoadTest {

    static int failures = 0;

    public static void main(String args[]) throws IOException {

        try {
            new BootSupport();
            throw new RuntimeException("Test configuration error - BootSupport loaded unexpectedly!");
        } catch (NoClassDefFoundError x) {
        }

        try {
            new AgentSupport();
            throw new RuntimeException("Test configuration error - AgentSupport loaded unexpectedly!");
        } catch (NoClassDefFoundError x) {
        }


        JarFile bootclasses = new JarFile("BootSupport.jar");
        JarFile agentclasses = new JarFile("AgentSupport.jar");

        Instrumentation ins = Agent.getInstrumentation();

        ins.appendToBootstrapClassLoaderSearch(bootclasses);
        try {
            new BootSupport();
            System.out.println("FAIL: BootSupport resolved");
            failures++;
        } catch (NoClassDefFoundError x) {
            System.out.println("PASS: BootSupport failed to resolve");
        }

        try {
            ins.appendToSystemClassLoaderSearch(agentclasses);
            try {
                new AgentSupport();
                System.out.println("FAIL: AgentSupport resolved");
                failures++;
            } catch (NoClassDefFoundError x) {
                System.out.println("PASS: AgentSupport failed to resolve");
            }
        } catch (UnsupportedOperationException x) {
            System.out.println("System class loader does not support adding to class path");
        }

        if (failures > 0) {
            throw new RuntimeException(failures + " test(s) failed.");
        }
    }
}
