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

/* @test id=satb
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm
 *      -Xbootclasspath/a:.
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahGCMode=satb
 *      gc.shenandoah.TestReferenceRefersToShenandoah
 */

/* @test id=iu
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm
 *      -Xbootclasspath/a:.
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      gc.shenandoah.TestReferenceRefersToShenandoah
 */

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
 *      gc.shenandoah.TestReferenceRefersToShenandoah
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
 *      gc.shenandoah.TestReferenceRefersToShenandoah
 */

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import sun.hotspot.WhiteBox;

public class TestReferenceRefersToShenandoah {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    private static final class TestObject {
        public final int value;

        public TestObject(int value) {
            this.value = value;
        }
    }

    private static volatile TestObject testObjectNone = null;
    private static volatile TestObject testObject1 = null;
    private static volatile TestObject testObject2 = null;
    private static volatile TestObject testObject3 = null;
    private static volatile TestObject testObject4 = null;

    private static ReferenceQueue<TestObject> queue = null;

    private static PhantomReference<TestObject> testPhantom1 = null;

    private static WeakReference<TestObject> testWeak2 = null;
    private static WeakReference<TestObject> testWeak3 = null;
    private static WeakReference<TestObject> testWeak4 = null;

    private static void setup() {
        testObjectNone = new TestObject(0);
        testObject1 = new TestObject(1);
        testObject2 = new TestObject(2);
        testObject3 = new TestObject(3);
        testObject4 = new TestObject(4);

        queue = new ReferenceQueue<TestObject>();

        testPhantom1 = new PhantomReference<TestObject>(testObject1, queue);

        testWeak2 = new WeakReference<TestObject>(testObject2, queue);
        testWeak3 = new WeakReference<TestObject>(testObject3, queue);
        testWeak4 = new WeakReference<TestObject>(testObject4, queue);
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
        gcUntilOld(testObjectNone);
        gcUntilOld(testObject1);
        gcUntilOld(testObject2);
        gcUntilOld(testObject3);
        gcUntilOld(testObject4);

        gcUntilOld(testPhantom1);

        gcUntilOld(testWeak2);
        gcUntilOld(testWeak3);
        gcUntilOld(testWeak4);
    }

    private static void progress(String msg) {
        System.out.println(msg);
    }

    private static void fail(String msg) throws Exception {
        throw new RuntimeException(msg);
    }

    private static void expectCleared(Reference<TestObject> ref,
                                      String which) throws Exception {
        expectNotValue(ref, testObjectNone, which);
        if (!ref.refersTo(null)) {
            fail("expected " + which + " to be cleared");
        }
    }

    private static void expectNotCleared(Reference<TestObject> ref,
                                         String which) throws Exception {
        expectNotValue(ref, testObjectNone, which);
        if (ref.refersTo(null)) {
            fail("expected " + which + " to not be cleared");
        }
    }

    private static void expectValue(Reference<TestObject> ref,
                                    TestObject value,
                                    String which) throws Exception {
        expectNotValue(ref, testObjectNone, which);
        expectNotCleared(ref, which);
        if (!ref.refersTo(value)) {
            fail(which + " doesn't refer to expected value");
        }
    }

    private static void expectNotValue(Reference<TestObject> ref,
                                       TestObject value,
                                       String which) throws Exception {
        if (ref.refersTo(value)) {
            fail(which + " refers to unexpected value");
        }
    }

    private static void checkInitialStates() throws Exception {
        expectValue(testPhantom1, testObject1, "testPhantom1");
        expectValue(testWeak2, testObject2, "testWeak2");
        expectValue(testWeak3, testObject3, "testWeak3");
        expectValue(testWeak4, testObject4, "testWeak4");
    }

    private static void discardStrongReferences() {
        // testObjectNone not dropped
        testObject1 = null;
        testObject2 = null;
        // testObject3 not dropped
        testObject4 = null;
    }

    private static boolean isShenandoahIUMode() {
        return "iu".equals(WB.getStringVMFlag("ShenandoahGCMode"));
    }

    private static void testConcurrentCollection() throws Exception {
        progress("setup concurrent collection test");
        setup();
        progress("gcUntilOld");
        gcUntilOld();

        progress("acquire control of concurrent cycles");
        WB.concurrentGCAcquireControl();
        try {
            progress("check initial states");
            checkInitialStates();

            progress("discard strong references");
            discardStrongReferences();

            progress("run GC to before marking completed");
            WB.concurrentGCRunTo(WB.BEFORE_MARKING_COMPLETED);

            progress("fetch test objects, possibly keeping some alive");
            expectNotCleared(testPhantom1, "testPhantom1");
            expectNotCleared(testWeak2, "testWeak2");
            expectValue(testWeak3, testObject3, "testWeak3");

            // For some collectors, calling get() will keep testObject4 alive.
            if (testWeak4.get() == null) {
                fail("testWeak4 unexpectedly == null");
            }

            progress("finish collection");
            WB.concurrentGCRunToIdle();

            progress("verify expected clears");
            expectCleared(testPhantom1, "testPhantom1");
            expectCleared(testWeak2, "testWeak2");
            expectValue(testWeak3, testObject3, "testWeak3");
            // This is true for all currently supported concurrent collectors,
            // except Shenandoah+IU, which allows clearing refs even when
            // accessed during concurrent marking.
            if (isShenandoahIUMode()) {
              expectCleared(testWeak4, "testWeak4");
            } else {
              expectNotCleared(testWeak4, "testWeak4");
            }

            progress("verify get returns expected values");
            if (testWeak2.get() != null) {
                fail("testWeak2.get() != null");
            }

            TestObject obj3 = testWeak3.get();
            if (obj3 == null) {
                fail("testWeak3.get() returned null");
            } else if (obj3.value != 3) {
                fail("testWeak3.get().value is " + obj3.value);
            }

            TestObject obj4 = testWeak4.get();
            if (!isShenandoahIUMode()) {
                if (obj4 == null) {
                    fail("testWeak4.get() returned null");
                } else if (obj4.value != 4) {
                    fail("testWeak4.get().value is " + obj4.value);
                }
            }

            progress("verify queue entries");
            long timeout = 60000; // 1 minute of milliseconds.
            while (true) {
                Reference<? extends TestObject> ref = queue.remove(timeout);
                if (ref == null) {
                    break;
                } else if (ref == testPhantom1) {
                    testPhantom1 = null;
                } else if (ref == testWeak2) {
                    testWeak2 = null;
                } else if (ref == testWeak3) {
                    testWeak3 = null;
                } else if (ref == testWeak4) {
                    testWeak4 = null;
                } else {
                    fail("unexpected reference in queue");
                }
            }
            if (testPhantom1 != null) {
                fail("testPhantom1 not notified");
            } else if (testWeak2 != null) {
                fail("testWeak2 not notified");
            } else if (testWeak3 == null) {
                fail("testWeak3 notified");
            } else if (testWeak4 == null) {
                if (obj4 != null) {
                    fail("testWeak4 notified");
                }
            }

        } finally {
            progress("release control of concurrent cycles");
            WB.concurrentGCReleaseControl();
        }
        progress("finished concurrent collection test");
    }

    private static void testSimpleCollection() throws Exception {
        progress("setup simple collection test");
        setup();
        progress("gcUntilOld");
        gcUntilOld();

        progress("check initial states");
        checkInitialStates();

        progress("discard strong references");
        TestObject tw4 = testWeak4.get(); // Keep testObject4 alive.
        discardStrongReferences();

        progress("collect garbage");
        WB.fullGC();

        progress("verify expected clears");
        expectCleared(testPhantom1, "testPhantom1");
        expectCleared(testWeak2, "testWeak2");
        expectValue(testWeak3, testObject3, "testWeak3");
        expectNotCleared(testWeak4, "testWeak4");

        progress("verify get returns expected values");
        if (testWeak2.get() != null) {
            fail("testWeak2.get() != null");
        } else if (testWeak3.get() != testObject3) {
            fail("testWeak3.get() is not expected value");
        } else if (testWeak4.get() != tw4) {
            fail("testWeak4.get() is not expected value");
        }

        progress("finished simple collection test");
    }

    public static void main(String[] args) throws Exception {
        if (WB.supportsConcurrentGCBreakpoints()) {
            testConcurrentCollection();
        }
        testSimpleCollection();
    }
}
