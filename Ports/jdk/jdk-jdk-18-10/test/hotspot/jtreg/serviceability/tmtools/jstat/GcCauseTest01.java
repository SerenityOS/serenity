/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test checks output displayed with jstat -gccause.
 *          Test scenario:
 *          test several times provokes garbage collection in the debuggee application and after each garbage
 *          collection runs jstat. jstat should show that after garbage collection number of GC events and garbage
 *          collection time increase.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @library ../share
 * @requires vm.opt.ExplicitGCInvokesConcurrent != true
 * @requires vm.gc != "Z" & vm.gc != "Shenandoah"
 * @run main/othervm -XX:+UsePerfData -Xmx128M GcCauseTest01
 */
import utils.*;

public class GcCauseTest01 {

    public static void main(String[] args) throws Exception {

        // We will be running "jstat -gc" tool
        JstatGcCauseTool jstatGcTool = new JstatGcCauseTool(ProcessHandle.current().pid());

        // Run once and get the  results asserting that they are reasonable
        JstatGcCauseResults measurement1 = jstatGcTool.measure();
        measurement1.assertConsistency();

        GcProvoker gcProvoker = new GcProvoker();

        // Provoke GC then run the tool again and get the results  asserting that they are reasonable
        gcProvoker.provokeGc();
        JstatGcCauseResults measurement2 = jstatGcTool.measure();
        measurement2.assertConsistency();

        // Assert the increase in GC events and time between the measurements
        JstatResults.assertGCEventsIncreased(measurement1, measurement2);
        JstatResults.assertGCTimeIncreased(measurement1, measurement2);

        // Provoke GC 3rd time then run the tool 3rd time twice and get the results
        // asserting that they are reasonable
        gcProvoker.provokeGc();
        JstatGcCauseResults measurement3 = jstatGcTool.measure();
        measurement3.assertConsistency();

        // Assert the increase in GC events and time between the measurements
        JstatResults.assertGCEventsIncreased(measurement2, measurement3);
        JstatResults.assertGCTimeIncreased(measurement2, measurement3);
    }
}
