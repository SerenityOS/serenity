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

/* @test
 * @bug 8240696
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm
 *      -Xbootclasspath/a:.
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      gc.TestReferenceClearDuringMarking
 */

import java.lang.ref.WeakReference;
import sun.hotspot.WhiteBox;

public class TestReferenceClearDuringMarking {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    private static Object testA = new Object();
    private static Object testB = new Object();

    private static final WeakReference<Object> refA1 = new WeakReference<Object>(testA);
    private static final WeakReference<Object> refA2 = new WeakReference<Object>(testA);

    private static final WeakReference<Object> refB1 = new WeakReference<Object>(testB);
    private static final WeakReference<Object> refB2 = new WeakReference<Object>(testB);

    private static void test() {
        while (!WB.isObjectInOldGen(testA) ||
               !WB.isObjectInOldGen(testB) ||
               !WB.isObjectInOldGen(refA1) ||
               !WB.isObjectInOldGen(refA2) ||
               !WB.isObjectInOldGen(refB1) ||
               !WB.isObjectInOldGen(refB2)) {
            WB.fullGC();
        }

        WB.concurrentGCAcquireControl();
        try {
            testA = null;
            testB = null;

            WB.concurrentGCRunTo(WB.AFTER_MARKING_STARTED);
            // Clear A1 early in marking, before reference discovery.
            refA1.clear();

            WB.concurrentGCRunTo(WB.BEFORE_MARKING_COMPLETED);
            // Clear B1 late in marking, after reference discovery.
            refB1.clear();

            WB.concurrentGCRunToIdle();

            // Verify that A2 and B2 still cleared by GC, i.e. the preceding
            // clear operations did not extend the lifetime of the referents.
            if (!refA2.refersTo(null)) {
                throw new RuntimeException("refA2 not cleared");
            }
            if (!refB2.refersTo(null)) {
                throw new RuntimeException("refB2 not cleared");
            }
        } finally {
            WB.concurrentGCReleaseControl();
        }
    }

    public static void main(String[] args) {
        if (WB.supportsConcurrentGCBreakpoints()) {
            test();
        }
    }
}
