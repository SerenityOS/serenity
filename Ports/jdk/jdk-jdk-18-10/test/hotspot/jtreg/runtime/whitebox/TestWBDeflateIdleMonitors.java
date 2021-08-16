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

package runtime.whitebox;

/*
 * @test
 * @bug 8246477
 * @summary Test to verify that WB method deflateIdleMonitors works correctly.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver runtime.whitebox.TestWBDeflateIdleMonitors
 */
import jdk.test.lib.Asserts;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;

public class TestWBDeflateIdleMonitors {
    static final int N_DELAY = 1000;  // delay between tries
    static final int N_TRIES = 5;     // number of times to try deflation

    public static void main(String args[]) throws Exception {
        ProcessBuilder pb = ProcessTools.createTestJvm(
                "-Xbootclasspath/a:.",
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI",
                "-Xlog:monitorinflation=info",
                InflateMonitorsTest.class.getName());

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        System.out.println(output.getStdout());
        output.shouldHaveExitValue(0);
        output.shouldContain("WhiteBox initiated DeflateIdleMonitors");
    }

    public static class InflateMonitorsTest {
        static WhiteBox wb = WhiteBox.getWhiteBox();
        public static Object obj;

        public static void main(String args[]) {
            obj = new Object();
            synchronized (obj) {
                // HotSpot implementation detail: asking for the hash code
                // when the object is locked causes monitor inflation.
                if (obj.hashCode() == 0xBAD) System.out.println("!");
                Asserts.assertEQ(wb.isMonitorInflated(obj), true,
                                 "Monitor should be inflated.");
            }
            for (int cnt = 1; cnt <= N_TRIES; cnt++) {
                System.out.println("Deflation try #" + cnt);
                boolean did_deflation = wb.deflateIdleMonitors();
                Asserts.assertEQ(did_deflation, true,
                                 "deflateIdleMonitors() should have worked.");
                if (!wb.isMonitorInflated(obj)) {
                    // Deflation worked so no more retries needed.
                    break;
                }
                try {
                    System.out.println("Deflation try #" + cnt + " failed. "
                                       + "Delaying before retry.");
                    Thread.sleep(N_DELAY);
                } catch (InterruptedException ie) {
                }
            }
            Asserts.assertEQ(wb.isMonitorInflated(obj), false,
                             "Monitor should be deflated.");
        }
    }
}
