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

import utils.*;
/*
 * @test
 * @summary Test checks output displayed with jstat -gcnew.
 *          Test scenario:
 *          test several times provokes garbage collection in the debuggee application and after each garbage
 *          collection runs jstat. jstat should show that after garbage collection number of GC events and garbage
 *          collection time increase.
 * @requires vm.gc != "Z" & vm.gc != "Shenandoah"
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @library ../share
 * @run main/othervm -XX:+UsePerfData -Xmx128M GcNewTest
 */

public class GcNewTest {

    public static void main(String[] args) throws Exception {

        // We will be running "jstat -gc" tool
        JstatGcNewTool jstatGcTool = new JstatGcNewTool(ProcessHandle.current().pid());

        // Run once and get the  results asserting that they are reasonable
        JstatGcNewResults measurement1 = jstatGcTool.measure();
        measurement1.assertConsistency();

        GcProvoker gcProvoker = new GcProvoker();

        // Provoke GC and run the tool again
        gcProvoker.provokeGc();
        JstatGcNewResults measurement2 = jstatGcTool.measure();
        measurement2.assertConsistency();

        // Assert the increase in GC events and time between the measurements
        assertThat(measurement2.getFloatValue("YGC") > measurement1.getFloatValue("YGC"), "YGC didn't increase between measurements 1 and 2");
        assertThat(measurement2.getFloatValue("YGCT") > measurement1.getFloatValue("YGCT"), "YGCT time didn't increase between measurements 1 and 2");

        // Provoke GC and run the tool again
        gcProvoker.provokeGc();
        JstatGcNewResults measurement3 = jstatGcTool.measure();
        measurement3.assertConsistency();

        // Assert the increase in GC events and time between the measurements
        assertThat(measurement3.getFloatValue("YGC") > measurement2.getFloatValue("YGC"), "YGC didn't increase between measurements 1 and 2");
        assertThat(measurement3.getFloatValue("YGCT") > measurement2.getFloatValue("YGCT"), "YGCT time didn't increase between measurements 1 and 2");

    }

    private static void assertThat(boolean result, String message) {
        if (!result) {
            throw new RuntimeException(message);
        };
    }
}
