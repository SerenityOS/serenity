/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4842196
 * @summary Test that there is no difference between the JMX version and the
 * JDK version when the application is run with a security manager and the
 * test codebase has the java permission to read the "java.runtime.version"
 * system property.
 * @author Luis-Miguel Alventosa
 *
 * @run clean ImplVersionTest ImplVersionCommand
 * @run build ImplVersionTest ImplVersionCommand ImplVersionReader
 * @run main ImplVersionTest
 */

import java.io.File;

public class ImplVersionTest {

    public static void main(String[] args) {
        try {
            // Get OS name
            //
            String osName = System.getProperty("os.name");
            System.out.println("osName = " + osName);
            if ("Windows 98".equals(osName)) {
                // Disable this test on Win98 due to parsing
                // errors (bad handling of white spaces) when
                // J2SE is installed under "Program Files".
                //
                System.out.println("This test is disabled on Windows 98.");
                System.out.println("Bye! Bye!");
                return;
            }
            // Get Java Home
            //
            String javaHome = System.getProperty("java.home");
            System.out.println("javaHome = " + javaHome);
            // Get test src
            //
            String testSrc = System.getProperty("test.src");
            System.out.println("testSrc = " + testSrc);
            // Get test classes
            //
            String testClasses = System.getProperty("test.classes");
            System.out.println("testClasses = " + testClasses);
            // Get boot class path
            //
            String command =
                javaHome + File.separator + "bin" + File.separator + "java " +
                " -classpath " + testClasses +
                " -Djava.security.manager -Djava.security.policy==" + testSrc +
                File.separator + "policy -Dtest.classes=" + testClasses +
                " ImplVersionCommand " +
                System.getProperty("java.runtime.version");
            System.out.println("ImplVersionCommand Exec Command = " +command);
            Process proc = Runtime.getRuntime().exec(command);
            new ImplVersionReader(proc, proc.getInputStream()).start();
            new ImplVersionReader(proc, proc.getErrorStream()).start();
            int exitValue = proc.waitFor();
            System.out.println("ImplVersionCommand Exit Value = " +
                               exitValue);
            if (exitValue != 0) {
                System.out.println("TEST FAILED: Incorrect exit value " +
                                   "from ImplVersionCommand");
                System.exit(exitValue);
            }
            // Test OK!
            //
            System.out.println("Bye! Bye!");
        } catch (Exception e) {
            System.out.println("Unexpected exception caught = " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
