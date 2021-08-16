/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @bug 8238676
 * @summary Check that attempting to use the JNI invocation API from an
 *          atexit handler fails as expected without crashing.
 *
 * @library /test/lib
 * @run main/native TestAtExit
 */

public class TestAtExit {

    // Using a nested class that invokes an enclosing method makes it
    // easier to setup and use the native library.
    static class Tester {
        static {
            System.loadLibrary("atExit");
        }

        // Record the fact we are using System.exit for termination
        static native void setUsingSystemExit();

        public static void main(String[] args) throws Exception {
            if (args.length > 0) {
                setUsingSystemExit();
                System.exit(0);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // We mustn't load Tester in this VM so we exec by name.
        String main = "TestAtExit$Tester";

        String jlp = "-Djava.library.path=" + Utils.TEST_NATIVE_PATH;
        // First run will terminate via DestroyJavaVM
        OutputAnalyzer output = ProcessTools.executeTestJvm(jlp, main);
        output.shouldNotContain("Unexpected");
        output.shouldHaveExitValue(0);
        output.reportDiagnosticSummary();

        // Second run will terminate via System.exit()
        output = ProcessTools.executeTestJvm(jlp, main, "doExit");
        output.shouldNotContain("Unexpected");
        output.shouldHaveExitValue(0);
        output.reportDiagnosticSummary();
    }
}
