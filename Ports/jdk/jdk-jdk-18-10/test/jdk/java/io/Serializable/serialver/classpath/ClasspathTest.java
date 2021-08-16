/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4035147 4785472
 * @library /test/lib
 * @build jdk.test.lib.Utils
 * @build jdk.test.lib.Asserts
 * @build jdk.test.lib.JDKToolFinder
 * @build jdk.test.lib.JDKToolLauncher
 * @build jdk.test.lib.Platform
 * @build jdk.test.lib.process.*
 * @build ClasspathTest
 * @run main serialver.ClasspathTest
 * @summary Test the use of the -classpath switch in the serialver application.
 */

package serialver;

import java.io.File;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.ProcessTools;

public class ClasspathTest implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    int a;
    int b;

    public static void main(String args[]) throws Exception {
        JDKToolLauncher serialver =
                JDKToolLauncher.create("serialver")
                               .addToolArg("-classpath")
                               .addToolArg(System.getProperty("test.class.path"))
                               .addToolArg("serialver.ClasspathTest");
        Process p = ProcessTools.startProcess("serialver",
                        new ProcessBuilder(serialver.getCommand()));
        p.waitFor();
        if (p.exitValue() != 0) {
            throw new RuntimeException("error occurs in serialver");
        }
    }
}
