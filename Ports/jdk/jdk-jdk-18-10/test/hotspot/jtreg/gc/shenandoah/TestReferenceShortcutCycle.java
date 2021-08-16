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

package gc.shenandoah;

/* @test id=satb-100
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @modules java.base
 * @run main jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm
 *      -Xbootclasspath/a:.
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahGCMode=satb -XX:ShenandoahGarbageThreshold=100 -Xmx100m
 *      gc.shenandoah.TestReferenceShortcutCycle
 */

/* @test id=iu-100
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @modules java.base
 * @run main jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm
 *      -Xbootclasspath/a:.
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGarbageThreshold=100 -Xmx100m
 *      gc.shenandoah.TestReferenceShortcutCycle
 */

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import sun.hotspot.WhiteBox;

public class TestReferenceShortcutCycle {
    private static final int NUM_ITEMS = 100000;

    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    private static final class TestObject {
        public final int value;

        public TestObject(int value) {
            this.value = value;
        }
    }

    private static WeakReference[] refs;

    private static void setup() {
        refs = new WeakReference[NUM_ITEMS];
        for (int i = 0; i < NUM_ITEMS; i++) {
            refs[i] = new WeakReference<>(new TestObject(i));
        }
    }

    private static void fail(String msg) throws Exception {
        throw new RuntimeException(msg);
    }

    private static void testConcurrentCollection() throws Exception {
        setup();
        WB.concurrentGCAcquireControl();
        try {
            WB.concurrentGCRunToIdle();
            WB.concurrentGCRunTo(WB.AFTER_CONCURRENT_REFERENCE_PROCESSING_STARTED);
            for (int i = 0; i < NUM_ITEMS; i++) {
                if (refs[i].get() != null) {
                    fail("resurrected referent");
                }
            }
        } finally {
            WB.concurrentGCReleaseControl();
        }
    }
    public static void main(String[] args) throws Exception {
        testConcurrentCollection();
    }
}
