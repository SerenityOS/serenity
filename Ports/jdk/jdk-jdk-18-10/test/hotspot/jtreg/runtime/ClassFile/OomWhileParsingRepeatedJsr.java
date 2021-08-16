/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test OomWhileParsingRepeatedJsr
 * @summary Testing class file parser; specifically parsing
 *          a file with repeated JSR (jump local subroutine)
 *          bytecode command.
 * @bug 6878713
 * @bug 7030610
 * @bug 7037122
 * @bug 7123945
 * @bug 8016029
 * @requires vm.flagless
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.desktop
 *          java.management
 * @run driver OomWhileParsingRepeatedJsr
 */

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class OomWhileParsingRepeatedJsr {

    public static void main(String[] args) throws Exception {

        // ======= Configure the test
        String jarFile = System.getProperty("test.src") + "/testcase.jar";
        String className = "OOMCrashClass1960_2";

        // limit is 768MB in native words
        int mallocMaxTestWords = (1024 * 1024 * 768 / 4);
        if (Platform.is64bit())
            mallocMaxTestWords = (mallocMaxTestWords / 2);

        // ======= extract the test class
        ProcessBuilder pb = new ProcessBuilder(new String[] {
            JDKToolFinder.getJDKTool("jar"),
            "xvf", jarFile } );
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);

        // ======= execute the test
        pb = ProcessTools.createJavaProcessBuilder(
            "-cp", ".",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:MallocMaxTestWords=" + mallocMaxTestWords,
            className );

        output = new OutputAnalyzer(pb.start());
        output.shouldNotHaveExitValue(0);
        output.shouldContain("Cannot reserve enough memory");
    }
}

