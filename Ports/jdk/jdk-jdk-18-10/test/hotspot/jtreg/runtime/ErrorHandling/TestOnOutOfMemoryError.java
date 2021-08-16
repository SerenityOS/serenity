/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestOnOutOfMemoryError
 * @summary Test using single and multiple -XX:OnOutOfMemoryError=<cmd>
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver TestOnOutOfMemoryError
 * @bug 8078470 8177522
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestOnOutOfMemoryError {

    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            // This should guarantee to throw:
            //  java.lang.OutOfMemoryError: Requested array size exceeds VM limit
            Object[] oa = new Object[Integer.MAX_VALUE];
            return;
        }

        // else this is the main test
        String msg1 = "Test1 Succeeded";
        String msg2 = "Test2 Succeeded";
        ProcessBuilder pb_single = ProcessTools.createJavaProcessBuilder(
           "-XX:OnOutOfMemoryError=echo " + msg1,
           TestOnOutOfMemoryError.class.getName(),
           "throwOOME");

        ProcessBuilder pb_multiple = ProcessTools.createJavaProcessBuilder(
           "-XX:OnOutOfMemoryError=echo " + msg1,
           "-XX:OnOutOfMemoryError=echo " + msg2,
           TestOnOutOfMemoryError.class.getName(),
           "throwOOME");

        OutputAnalyzer output_single = new OutputAnalyzer(pb_single.start());

        OutputAnalyzer output_multiple = new OutputAnalyzer(pb_multiple.start());

        /* Actual output should look like this:
           #
           # java.lang.OutOfMemoryError: Requested array size exceeds VM limit
           # -XX:OnOutOfMemoryError="echo Test Succeeded"
           #   Executing /bin/sh -c "echo Test Succeeded"...
           Test Succeeded
           Exception in thread "main" java.lang.OutOfMemoryError: Requested array size exceeds VM limit
           at OOME.main(OOME.java:3)

           So we don't want to match on the "# Executing ..." line, and they
           both get written to stdout.
        */
        output_single.shouldContain("Requested array size exceeds VM limit");
        output_single.stdoutShouldMatch("^" + msg1); // match start of line only

        output_multiple.shouldContain("Requested array size exceeds VM limit");
        output_multiple.stdoutShouldMatch("^" + msg1); // match start of line only
        output_multiple.stdoutShouldMatch("^" + msg2); // match start of line only

        System.out.println("PASSED");
    }
}
