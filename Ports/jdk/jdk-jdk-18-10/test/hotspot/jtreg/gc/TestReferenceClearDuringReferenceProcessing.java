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
 * @bug 8256517
 * @requires vm.gc.Z | vm.gc.Shenandoah
 * @requires vm.gc != "null"
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm
 *      -Xbootclasspath/a:.
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      gc.TestReferenceClearDuringReferenceProcessing
 */

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import sun.hotspot.WhiteBox;

public class TestReferenceClearDuringReferenceProcessing {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    private static Object testObject = new Object();
    private static final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
    private static final WeakReference<Object> ref = new WeakReference<Object>(testObject, queue);

    private static final long TIMEOUT = 10000; // 10sec in millis

    private static void test() {
        while (!WB.isObjectInOldGen(testObject) ||
               !WB.isObjectInOldGen(ref)) {
            WB.fullGC();
        }

        WB.concurrentGCAcquireControl();
        try {
            testObject = null;
            WB.concurrentGCRunTo(WB.AFTER_CONCURRENT_REFERENCE_PROCESSING_STARTED);
            if (!ref.refersTo(null)) {
                throw new RuntimeException("ref not apparently cleared");
            }

            ref.clear();

            WB.concurrentGCRunToIdle();

            Reference<? extends Object> enqueued = null;

            try {
                enqueued = queue.remove(TIMEOUT);
            } catch (InterruptedException e) {
                throw new RuntimeException("queue.remove interrupted");
            }
            if (enqueued == null) {
                throw new RuntimeException("ref not enqueued");
            } else if (enqueued != ref) {
                throw new RuntimeException("some other ref enqeueued");
            }
        } finally {
            WB.concurrentGCReleaseControl();
        }
    }

    public static void main(String[] args) {
        if (WB.supportsConcurrentGCBreakpoints()) {
            // Also requires concurrent reference processing, but we
            // don't have a predicate for that.  For now,
            // use @requires and CLA to limit the applicable collectors.
            test();
        }
    }
}
