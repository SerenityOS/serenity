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
 *          test forces debuggee application call System.gc(), runs jstat and checks that
 *          cause of last garbage collection displayed by jstat (LGCC) is 'System.gc()'.
 * @requires vm.gc != "Z" & vm.gc != "Shenandoah"
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @library ../share
 * @run main/othervm -XX:+UsePerfData -Xmx128M -XX:MaxMetaspaceSize=128M GcCauseTest03
 */
import utils.*;

public class GcCauseTest03 {

    private final static float targetMemoryUsagePercent = 0.7f;

    public static void main(String[] args) throws Exception {

        // We will be running "jstat -gc" tool
        JstatGcCauseTool jstatGcTool = new JstatGcCauseTool(ProcessHandle.current().pid());

        System.gc();

        // Run once and get the  results asserting that they are reasonable
        JstatGcCauseResults measurement = jstatGcTool.measure();
        measurement.assertConsistency();

        if (measurement.valueExists("LGCC")) {
            if (!"System.gc()".equals(measurement.getStringValue("LGCC"))) {
                throw new RuntimeException("Unexpected GC cause: " + measurement.getStringValue("LGCC") + ", expected System.gc()");
            }
        }

    }
}
