/*
* Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
* Copyright (c) 2020, Arm Limited. All rights reserved.
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
*
*/

/**
 * @test
 *
 * @requires os.arch == "aarch64" & vm.compiler2.enabled & os.family == "linux"
 * @summary Verify VM SVE checking behavior
 * @library /test/lib
 * @requires vm.flagless
 *
 * @run main/othervm/native compiler.c2.aarch64.TestSVEWithJNI
 */

package compiler.c2.aarch64;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestSVEWithJNI {
    static {
        System.loadLibrary("TestSVEWithJNI");
    }

    static final int EXIT_CODE = 99;
    // Returns a nonnegative on success, or a negative value on error.
    public static native int setVectorLength(int arg);
    // Returns a nonnegative value on success, or a negative value on error.
    public static native int getVectorLength();

    public static final String MSG = "Current Vector Size: ";
    public static void testNormal() {
        int vlen = getVectorLength();
        System.out.println(MSG + vlen);
        // Should be fine if no vector length changed.
        if (setVectorLength(vlen) < 0) {
            throw new Error("Error in setting vector length.");
        }
    }

    public static void testAbort() {
        int vlen = getVectorLength();
        if (vlen <= 16) {
            throw new Error("Error: unsupported vector length.");
        }
        if (setVectorLength(16) < 0) {
            throw new Error("Error: setting vector length failed.");
        }
    }

    public static ProcessBuilder createProcessBuilder(String [] args, String mode) {
        List<String> vmopts = new ArrayList<>();
        String testjdkPath = System.getProperty("test.jdk");
        Collections.addAll(vmopts, "-Dtest.jdk=" + testjdkPath);
        Collections.addAll(vmopts, args);
        Collections.addAll(vmopts, TestSVEWithJNI.class.getName(), mode);
        return ProcessTools.createJavaProcessBuilder(vmopts.toArray(new String[vmopts.size()]));
    }

    public static void main(String [] args) throws Exception {
        if (args.length == 0) {
            int vlen = getVectorLength();
            if (vlen < 0) {
                return;
            }
            String [][] testOpts = {
                {"-Xint", "-XX:UseSVE=1"},
                {"-Xcomp", "-XX:UseSVE=1"},
            };
            ProcessBuilder pb;
            OutputAnalyzer output;
            for (String [] opts : testOpts) {
                pb = createProcessBuilder(opts, "normal");
                output = new OutputAnalyzer(pb.start());
                output.shouldHaveExitValue(EXIT_CODE);

                pb = createProcessBuilder(opts, "abort");
                output = new OutputAnalyzer(pb.start());
                output.shouldNotHaveExitValue(EXIT_CODE);
                output.shouldMatch("(error|Error|ERROR)");
            }

            // Verify MaxVectorSize

            // Any SVE architecture should support 128-bit vector size.
            pb = createProcessBuilder(new String []{"-XX:UseSVE=1", "-XX:MaxVectorSize=16"}, "normal");
            output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(EXIT_CODE);
            output.shouldContain(MSG + 16);

            // An unsupported large vector size value.
            pb = createProcessBuilder(new String []{"-XX:UseSVE=1", "-XX:MaxVectorSize=512"}, "normal");
            output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(EXIT_CODE);
            output.shouldContain("warning");
        } else if (args[0].equals("normal")) {
            testNormal();
            System.exit(EXIT_CODE);
        } else if (args[0].equals("abort")) {
            testAbort();
            System.exit(EXIT_CODE);
        }
    }
}
