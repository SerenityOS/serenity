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
 * @requires vm.gc != "Shenandoah" | vm.opt.ShenandoahGCMode != "iu"
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm
 *      -Xbootclasspath/a:.
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      gc.TestReferenceRefersToDuringConcMark
 */

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import sun.hotspot.WhiteBox;

public class TestReferenceRefersToDuringConcMark {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    private static volatile Object testObject = null;

    private static WeakReference<Object> testWeak = null;

    private static void setup() {
        testObject = new Object();
        testWeak = new WeakReference<Object>(testObject);
    }

    private static void gcUntilOld(Object o) throws Exception {
        if (!WB.isObjectInOldGen(o)) {
            WB.fullGC();
            if (!WB.isObjectInOldGen(o)) {
                fail("object not promoted by full gc");
            }
        }
    }

    private static void gcUntilOld() throws Exception {
        gcUntilOld(testObject);
        gcUntilOld(testWeak);
    }

    private static void fail(String msg) throws Exception {
        throw new RuntimeException(msg);
    }

    private static void expectNotCleared(Reference<Object> ref,
                                         String which) throws Exception {
        if (ref.refersTo(null)) {
            fail("expected " + which + " to not be cleared");
        }
    }

    private static void expectValue(Reference<Object> ref,
                                    Object value,
                                    String which) throws Exception {
        expectNotCleared(ref, which);
        if (!ref.refersTo(value)) {
            fail(which + " doesn't refer to expected value");
        }
    }

    private static void checkInitialStates() throws Exception {
        expectValue(testWeak, testObject, "testWeak");
    }

    private static void discardStrongReferences() {
        testObject = null;
    }

    private static void testConcurrentCollection() throws Exception {
        setup();
        gcUntilOld();

        WB.concurrentGCAcquireControl();
        try {
            checkInitialStates();

            discardStrongReferences();

            WB.concurrentGCRunTo(WB.BEFORE_MARKING_COMPLETED);

            // For most collectors - the configurations tested here -,
            // calling get() will keep testObject alive.
            if (testWeak.get() == null) {
                fail("testWeak unexpectedly == null");
            }

            WB.concurrentGCRunToIdle();

            expectNotCleared(testWeak, "testWeak");
        } finally {
            WB.concurrentGCReleaseControl();
        }
    }

    public static void main(String[] args) throws Exception {
        if (WB.supportsConcurrentGCBreakpoints()) {
            testConcurrentCollection();
        }
    }
}
