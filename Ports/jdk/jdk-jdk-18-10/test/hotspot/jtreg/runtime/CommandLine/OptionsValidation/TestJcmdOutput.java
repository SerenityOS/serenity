/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verify jcmd error message for out-of-range value and for
 *          value which is not allowed by constraint. Also check that
 *          jcmd does not print an error message to the target process output.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          jdk.management
 * @run driver TestJcmdOutput
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.management.DynamicVMOption;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.dcmd.PidJcmdExecutor;

public class TestJcmdOutput {

    /* Message printed by jcmd for value which is out-of-range */
    static final String JCMD_OUT_OF_RANGE_MESSAGE = "error: must have value in range";
    /* Message printed by jcmd for value which is not allowed by constraint */
    static final String JCMD_CONSTRAINT_MESSAGE = "value violates its flag's constraint";

    public static void main(String[] args) throws Exception {
        OutputAnalyzer output;

        System.out.println("Verify jcmd error message and that jcmd does not write errors to the target process output");
        output = new OutputAnalyzer((ProcessTools.createJavaProcessBuilder(
                "-Dtest.jdk=" + System.getProperty("test.jdk"),
                "-XX:MinHeapFreeRatio=20", "-XX:MaxHeapFreeRatio=80", runJcmd.class.getName())).start());

        output.shouldHaveExitValue(0);
        /* Verify that jcmd not print error message to the target process output */
        output.shouldNotContain(JCMD_OUT_OF_RANGE_MESSAGE);
        output.shouldNotContain(JCMD_CONSTRAINT_MESSAGE);
    }

    public static class runJcmd {

        public static void main(String[] args) throws Exception {
            int minHeapFreeRatio = Integer.valueOf((new DynamicVMOption("MinHeapFreeRatio")).getValue());
            int maxHeapFreeRatio = Integer.valueOf((new DynamicVMOption("MaxHeapFreeRatio")).getValue());
            PidJcmdExecutor executor = new PidJcmdExecutor();

            Asserts.assertGT(minHeapFreeRatio, 0, "MinHeapFreeRatio must be greater than 0");
            Asserts.assertLT(maxHeapFreeRatio, 100, "MaxHeapFreeRatio must be less than 100");

            /* Check out-of-range values */
            executor.execute("VM.set_flag MinHeapFreeRatio -1", true).shouldContain(JCMD_OUT_OF_RANGE_MESSAGE);
            executor.execute("VM.set_flag MaxHeapFreeRatio 101", true).shouldContain(JCMD_OUT_OF_RANGE_MESSAGE);

            /* Check values which not allowed by constraint */
            executor.execute(
                    String.format("VM.set_flag MinHeapFreeRatio %d", maxHeapFreeRatio + 1), true)
                    .shouldContain(JCMD_CONSTRAINT_MESSAGE);
            executor.execute(
                    String.format("VM.set_flag MaxHeapFreeRatio %d", minHeapFreeRatio - 1), true)
                    .shouldContain(JCMD_CONSTRAINT_MESSAGE);
        }
    }
}
