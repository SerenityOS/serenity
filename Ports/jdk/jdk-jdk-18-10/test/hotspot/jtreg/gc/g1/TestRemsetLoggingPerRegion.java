/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1;

/*
 * @test TestRemsetLoggingPerRegion.java
 * @requires vm.gc.G1
 * @bug 8014078 8129977 8145534
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 *          java.management/sun.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @summary Verify output of -Xlog:gc+remset*=trace in regards to per-region type output
 * @run driver gc.g1.TestRemsetLoggingPerRegion
 */

public class TestRemsetLoggingPerRegion {

    public static void main(String[] args) throws Exception {
        String result;

        // single remembered set summary output at the end
        result = TestRemsetLoggingTools.runTest(new String[] { "-Xlog:gc+remset*=trace" }, 0);
        TestRemsetLoggingTools.expectPerRegionRSetSummaries(result, 1, 0);

        // two times remembered set summary output
        result = TestRemsetLoggingTools.runTest(new String[] { "-Xlog:gc+remset*=trace", "-XX:G1SummarizeRSetStatsPeriod=1" }, 1);
        TestRemsetLoggingTools.expectPerRegionRSetSummaries(result, 1, 2);
    }
}
