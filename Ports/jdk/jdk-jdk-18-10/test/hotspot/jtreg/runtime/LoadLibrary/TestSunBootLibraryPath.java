/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestSunBootLibraryPath.java
 * @bug 8227021
 * @summary Confirm using too-long paths in sun.boot.library.path causes failure and useful error message.
 * @author afarley
 * @library /test/lib
 * @build jdk.test.lib.process.ProcessTools
 * @run driver TestSunBootLibraryPath
 */

import jdk.test.lib.process.ProcessTools;

public class TestSunBootLibraryPath {
    static String expectedErrorMessage = "The VM tried to use a path that exceeds the maximum path length for this system.";

    public static void main(String[] args) throws Exception {
        // Allows us to re-use this class as a do-nothing test class simply by passing a "Do-Nothing" argument.
        if (args.length == 0) {
            // Grab any path.
            String tooLongPath = System.getProperty("sun.boot.library.path");
            // Add enough characters to make it "too long".
            tooLongPath += "a".repeat(5000);
            // Start a java process with this property set, and check that:
            // 1) The process failed and
            // 2) The error message was correct.
            ProcessTools.executeTestJvm("-Dsun.boot.library.path=" + tooLongPath,
                                        "TestSunBootLibraryPath",
                                        "'Do-Nothing'")
                                        .shouldNotHaveExitValue(0)
                                        .stdoutShouldContain(expectedErrorMessage);
        } else if (!args[0].equals("Do-Nothing")) {
            // Fail, to prevent accidental args from causing accidental test passing.
            throw new IllegalArgumentException("Test was launched with an invalid argument.");
        }
    }
}
