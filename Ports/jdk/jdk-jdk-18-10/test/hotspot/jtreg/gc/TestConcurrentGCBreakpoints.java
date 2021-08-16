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

package gc;

/*
 * @test TestConcurrentGCBreakpoints
 * @summary Test of WhiteBox concurrent GC control.
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm
 *   -Xbootclasspath/a:.
 *   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *   gc.TestConcurrentGCBreakpoints
 */

import sun.hotspot.WhiteBox;
import sun.hotspot.gc.GC;

public class TestConcurrentGCBreakpoints {

    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    // All testN() assume initial state is idle, and restore that state.

    // Step through the common breakpoints.
    private static void testSimpleCycle() throws Exception {
        System.out.println("testSimpleCycle");
        try {
            // Run one cycle.
            WB.concurrentGCRunTo(WB.AFTER_MARKING_STARTED);
            WB.concurrentGCRunTo(WB.BEFORE_MARKING_COMPLETED);
            WB.concurrentGCRunToIdle();
            // Run a second cycle.
            WB.concurrentGCRunTo(WB.AFTER_MARKING_STARTED);
            WB.concurrentGCRunTo(WB.BEFORE_MARKING_COMPLETED);
            WB.concurrentGCRunToIdle();
        } finally {
            WB.concurrentGCRunToIdle();
        }
    }

    // Verify attempted wraparound detected and reported.
    private static void testEndBeforeBreakpointError() throws Exception {
        System.out.println("testEndBeforeBreakpointError");
        try {
            WB.concurrentGCRunTo(WB.BEFORE_MARKING_COMPLETED);
            try {
                WB.concurrentGCRunTo(WB.AFTER_MARKING_STARTED);
            } catch (IllegalStateException e) {
                // Reached end of cycle before desired breakpoint.
            }
        } finally {
            WB.concurrentGCRunToIdle();
        }
    }

    // Verify attempted wraparound detected and reported without throw.
    private static void testEndBeforeBreakpoint() throws Exception {
        System.out.println("testEndBeforeBreakpoint");
        try {
            WB.concurrentGCRunTo(WB.BEFORE_MARKING_COMPLETED);
            if (WB.concurrentGCRunTo(WB.AFTER_MARKING_STARTED, false)) {
                throw new RuntimeException("Unexpected wraparound");
            }
        } finally {
            WB.concurrentGCRunToIdle();
        }
    }

    private static void testUnknownBreakpoint() throws Exception {
        System.out.println("testUnknownBreakpoint");
        try {
            if (WB.concurrentGCRunTo("UNKNOWN BREAKPOINT", false)) {
                throw new RuntimeException("RunTo UNKNOWN BREAKPOINT");
            }
        } finally {
            WB.concurrentGCRunToIdle();
        }
    }

    private static void test() throws Exception {
        try {
            System.out.println("taking control");
            WB.concurrentGCAcquireControl();
            testSimpleCycle();
            testEndBeforeBreakpointError();
            testEndBeforeBreakpoint();
            testUnknownBreakpoint();
        } finally {
            System.out.println("releasing control");
            WB.concurrentGCReleaseControl();
        }
    }

    private static boolean expectSupported() {
        return GC.G1.isSelected() ||
               GC.Z.isSelected() ||
               GC.Shenandoah.isSelected();
    }

    private static boolean expectUnsupported() {
        return GC.Serial.isSelected() ||
               GC.Parallel.isSelected() ||
               GC.Epsilon.isSelected();
    }

    public static void main(String[] args) throws Exception {
        boolean supported = WB.supportsConcurrentGCBreakpoints();
        if (expectSupported()) {
            if (supported) {
                test();
            } else {
                throw new RuntimeException("Expected support");
            }
        } else if (expectUnsupported()) {
            if (supported) {
                throw new RuntimeException("Unexpected support");
            }
        } else {
            throw new RuntimeException("Unknown GC");
        }
    }
}
