/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jdk.internal.ref.PhantomCleanable;
import jdk.internal.ref.CleanerFactory;

import sun.hotspot.WhiteBox;

import jdk.test.lib.Utils;

import org.testng.Assert;
import org.testng.TestNG;
import org.testng.annotations.Test;

/*
 * @test
 * @library /lib/testlibrary /test/lib
 * @build sun.hotspot.WhiteBox
 *        jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @modules java.base/jdk.internal.misc
 *          java.base/jdk.internal.ref
 *          java.management
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run testng/othervm
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      -verbose:gc CleanerTest
 */

@Test
public class CleanerTest {
    // A common CleaningService used by the test for notifications
    static final Cleaner COMMON = CleanerFactory.cleaner();

    // Access to WhiteBox utilities
    static final WhiteBox whitebox = WhiteBox.getWhiteBox();

    /**
     * Test that sequences of the various actions on a Reference
     * and on the Cleanable instance have the desired result.
     * The test cases are generated for each of phantom, weak and soft
     * references.
     * The sequence of actions includes all permutations to an initial
     * list of actions including clearing the ref and resulting garbage
     * collection actions on the reference and explicitly performing
     * the cleaning action.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testCleanableActions() {
        Cleaner cleaner = Cleaner.create();

        // Individually
        generateCases(cleaner, c -> c.clearRef());
        generateCases(cleaner, c -> c.doClean());

        // Pairs
        generateCases(cleaner, c -> c.doClean(), c -> c.clearRef());

        CleanableCase s = setupPhantom(COMMON, cleaner);
        cleaner = null;
        checkCleaned(s.getSemaphore(), true, "Cleaner was cleaned:");
    }

    /**
     * Test the jdk.internal.misc APIs with sequences of the various actions
     * on a Reference and on the Cleanable instance have the desired result.
     * The test cases are generated for each of phantom, weak and soft
     * references.
     * The sequence of actions includes all permutations to an initial
     * list of actions including clearing the ref and resulting garbage
     * collection actions on the reference, explicitly performing
     * the cleanup and explicitly clearing the cleaning action.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testRefSubtypes() {
        Cleaner cleaner = Cleaner.create();

        // Individually
        generateCasesInternal(cleaner, c -> c.clearRef());
        generateCasesInternal(cleaner, c -> c.doClean());
        generateCasesInternal(cleaner, c -> c.doClear());

        // Pairs
        generateCasesInternal(cleaner,
                c -> c.doClear(), c -> c.doClean());

        // Triplets
        generateCasesInternal(cleaner,
                c -> c.doClear(), c -> c.doClean(), c -> c.clearRef());

        generateExceptionCasesInternal(cleaner);

        CleanableCase s = setupPhantom(COMMON, cleaner);
        cleaner = null;
        checkCleaned(s.getSemaphore(), true, "Cleaner was cleaned:");
    }

    /**
     * Generate tests using the runnables for each of phantom, weak,
     * and soft references.
     * @param cleaner  the cleaner
     * @param runnables the sequence of actions on the test case
     */
    @SuppressWarnings("unchecked")
    void generateCases(Cleaner cleaner, Consumer<CleanableCase>... runnables) {
        generateCases(() -> setupPhantom(cleaner, null), runnables.length, runnables);
    }

    @SuppressWarnings("unchecked")
    void generateCasesInternal(Cleaner cleaner, Consumer<CleanableCase>... runnables) {
        generateCases(() -> setupPhantomSubclass(cleaner, null),
                runnables.length, runnables);
    }

    @SuppressWarnings("unchecked")
    void generateExceptionCasesInternal(Cleaner cleaner) {
        generateCases(() -> setupPhantomSubclassException(cleaner, null),
                1, c -> c.clearRef());
    }

    /**
     * Generate all permutations of the sequence of runnables
     * and test each one.
     * The permutations are generated using Heap, B.R. (1963) Permutations by Interchanges.
     * @param generator the supplier of a CleanableCase
     * @param n the first index to interchange
     * @param runnables the sequence of actions
     */
    @SuppressWarnings("unchecked")
    void generateCases(Supplier<CleanableCase> generator, int n,
                       Consumer<CleanableCase> ... runnables) {
        if (n == 1) {
            CleanableCase test = generator.get();
            try {
                verifyGetRef(test);

                // Apply the sequence of actions on the Ref
                for (Consumer<CleanableCase> c : runnables) {
                    c.accept(test);
                }
                verify(test);
            } catch (Exception e) {
                Assert.fail(test.toString(), e);
            }
        } else {
            for (int i = 0; i < n - 1; i += 1) {
                generateCases(generator, n - 1, runnables);
                Consumer<CleanableCase> t = runnables[n - 1];
                int ndx = ((n & 1) == 0) ? i : 0;
                runnables[n - 1] = runnables[ndx];
                runnables[ndx] = t;
            }
            generateCases(generator, n - 1, runnables);
        }
    }

    /**
     * Verify the test case.
     * Any actions directly on the Reference or Cleanable have been executed.
     * The CleanableCase under test is given a chance to do the cleanup
     * by forcing a GC.
     * The result is compared with the expected result computed
     * from the sequence of operations on the Cleanable.
     * The Cleanable itself should have been cleanedup.
     *
     * @param test A CleanableCase containing the references
     */
    void verify(CleanableCase test) {
        System.out.println(test);
        int r = test.expectedResult();

        CleanableCase cc = setupPhantom(COMMON, test.getCleanable());
        test.clearCleanable();        // release this hard reference

        checkCleaned(test.getSemaphore(),
                r == CleanableCase.EV_CLEAN,
                "Cleanable was cleaned:");
        checkCleaned(cc.getSemaphore(), true,
                "The reference to the Cleanable was freed:");
    }

    /**
     * Verify that the reference.get works (or not) as expected.
     * It handles the cases where UnsupportedOperationException is expected.
     *
     * @param test the CleanableCase
     */
    void verifyGetRef(CleanableCase test) {
        Reference<?> r = (Reference) test.getCleanable();
        try {
            Object o = r.get();
            Reference<?> expectedRef = test.getRef();
            Assert.assertEquals(expectedRef.get(), o,
                    "Object reference incorrect");
            if (r.getClass().getName().endsWith("CleanableRef")) {
                Assert.fail("should not be able to get referent");
            }
        } catch (UnsupportedOperationException uoe) {
            if (r.getClass().getName().endsWith("CleanableRef")) {
                // Expected exception
            } else {
                Assert.fail("Unexpected exception from subclassed cleanable: " +
                        uoe.getMessage() + ", class: " + r.getClass());
            }
        }
    }

    /**
     * Test that releasing the reference to the Cleaner service allows it to be
     * be freed.
     */
    @Test
    void testCleanerTermination() {
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        Cleaner service = Cleaner.create();

        PhantomReference<Object> ref = new PhantomReference<>(service, queue);
        System.gc();
        // Clear the Reference to the cleaning service and force a gc.
        service = null;
        System.gc();
        try {
            Reference<?> r = queue.remove(1000L);
            Assert.assertNotNull(r, "queue.remove timeout,");
            Assert.assertEquals(r, ref, "Wrong Reference dequeued");
        } catch (InterruptedException ie) {
            System.out.printf("queue.remove Interrupted%n");
        }
    }

    /**
     * Check a semaphore having been released by cleanup handler.
     * Force a number of GC cycles to give the GC a chance to process
     * the Reference and for the cleanup action to be run.
     * Use a larger number of cycles to wait for an expected cleaning to occur.
     *
     * @param semaphore a Semaphore
     * @param expectCleaned true if cleaning should occur
     * @param msg a message to explain the error
     */
    static void checkCleaned(Semaphore semaphore, boolean expectCleaned,
                             String msg) {
        long max_cycles = expectCleaned ? 10 : 3;
        long cycle = 0;
        for (; cycle < max_cycles; cycle++) {
            // Force GC
            whitebox.fullGC();

            try {
                if (semaphore.tryAcquire(Utils.adjustTimeout(10L), TimeUnit.MILLISECONDS)) {
                    System.out.printf(" Cleanable cleaned in cycle: %d%n", cycle);
                    Assert.assertEquals(true, expectCleaned, msg);
                    return;
                }
            } catch (InterruptedException ie) {
                // retry in outer loop
            }
        }
        // Object has not been cleaned
        Assert.assertEquals(false, expectCleaned, msg);
    }

    /**
     * Create a CleanableCase for a PhantomReference.
     * @param cleaner the cleaner to use
     * @param obj an object or null to create a new Object
     * @return a new CleanableCase preset with the object, cleanup, and semaphore
     */
    static CleanableCase setupPhantom(Cleaner cleaner, Object obj) {
        if (obj == null) {
            obj = new Object();
        }
        Semaphore s1 = new Semaphore(0);
        Cleaner.Cleanable c1 = cleaner.register(obj, () -> s1.release());

        return new CleanableCase(new PhantomReference<>(obj, null), c1, s1);
    }

    /**
     * Create a CleanableCase for a PhantomReference.
     * @param cleaner the cleaner to use
     * @param obj an object or null to create a new Object
     * @return a new CleanableCase preset with the object, cleanup, and semaphore
     */
    static CleanableCase setupPhantomSubclass(Cleaner cleaner, Object obj) {
        if (obj == null) {
            obj = new Object();
        }
        Semaphore s1 = new Semaphore(0);

        Cleaner.Cleanable c1 = new PhantomCleanable<Object>(obj, cleaner) {
            protected void performCleanup() {
                s1.release();
            }
        };

        return new CleanableCase(new PhantomReference<>(obj, null), c1, s1);
    }

    /**
     * Create a CleanableCase for a PhantomReference.
     * @param cleaner the cleaner to use
     * @param obj an object or null to create a new Object
     * @return a new CleanableCase preset with the object, cleanup, and semaphore
     */
    static CleanableCase setupPhantomSubclassException(Cleaner cleaner, Object obj) {
        if (obj == null) {
            obj = new Object();
        }
        Semaphore s1 = new Semaphore(0);

        Cleaner.Cleanable c1 = new PhantomCleanable<Object>(obj, cleaner) {
            protected void performCleanup() {
                s1.release();
                throw new RuntimeException("Exception thrown to cleaner thread");
            }
        };

        return new CleanableCase(new PhantomReference<>(obj, null), c1, s1, true);
    }

    /**
     * CleanableCase encapsulates the objects used for a test.
     * The reference to the object is not held directly,
     * but in a Reference object that can be cleared.
     * The semaphore is used to count whether the cleanup occurred.
     * It can be awaited on to determine that the cleanup has occurred.
     * It can be checked for non-zero to determine if it was
     * invoked or if it was invoked twice (a bug).
     */
    static class CleanableCase {

        private volatile Reference<?> ref;
        private volatile Cleaner.Cleanable cleanup;
        private final Semaphore semaphore;
        private final boolean throwsEx;
        private final int[] events;   // Sequence of calls to clean, clear, etc.
        private volatile int eventNdx;

        public static int EV_UNKNOWN = 0;
        public static int EV_CLEAR = 1;
        public static int EV_CLEAN = 2;
        public static int EV_UNREF = 3;
        public static int EV_CLEAR_CLEANUP = 4;


        CleanableCase(Reference<Object> ref, Cleaner.Cleanable cleanup,
                      Semaphore semaphore) {
            this.ref = ref;
            this.cleanup = cleanup;
            this.semaphore = semaphore;
            this.throwsEx = false;
            this.events = new int[4];
            this.eventNdx = 0;
        }
        CleanableCase(Reference<Object> ref, Cleaner.Cleanable cleanup,
                      Semaphore semaphore,
                      boolean throwsEx) {
            this.ref = ref;
            this.cleanup = cleanup;
            this.semaphore = semaphore;
            this.throwsEx = throwsEx;
            this.events = new int[4];
            this.eventNdx = 0;
        }

        public Reference<?> getRef() {
            return ref;
        }

        public void clearRef() {
            addEvent(EV_UNREF);
            ref.clear();
        }

        public Cleaner.Cleanable getCleanable() {
            return cleanup;
        }

        public void doClean() {
            try {
                addEvent(EV_CLEAN);
                cleanup.clean();
            } catch (RuntimeException ex) {
                if (!throwsEx) {
                    // unless it is known this case throws an exception, rethrow
                    throw ex;
                }
            }
        }

        public void doClear() {
            addEvent(EV_CLEAR);
            ((Reference)cleanup).clear();
        }

        public void clearCleanable() {
            addEvent(EV_CLEAR_CLEANUP);
            cleanup = null;
        }

        public Semaphore getSemaphore() {
            return semaphore;
        }

        public boolean isCleaned() {
            return semaphore.availablePermits() != 0;
        }

        private synchronized void addEvent(int e) {
            events[eventNdx++] = e;
        }

        /**
         * Computed the expected result from the sequence of events.
         * If EV_CLEAR appears before anything else, it is cleared.
         * If EV_CLEAN appears before EV_UNREF, then it is cleaned.
         * Anything else is Unknown.
         * @return EV_CLEAR if the cleanup should occur;
         *         EV_CLEAN if the cleanup should occur;
         *         EV_UNKNOWN if it is unknown.
         */
        public synchronized int expectedResult() {
            // Test if EV_CLEAR appears before anything else
            int clearNdx = indexOfEvent(EV_CLEAR);
            int cleanNdx = indexOfEvent(EV_CLEAN);
            int unrefNdx = indexOfEvent(EV_UNREF);
            if (clearNdx < cleanNdx) {
                return EV_CLEAR;
            }
            if (cleanNdx < clearNdx || cleanNdx < unrefNdx) {
                return EV_CLEAN;
            }
            if (unrefNdx < eventNdx) {
                return EV_CLEAN;
            }

            return EV_UNKNOWN;
        }

        private synchronized  int indexOfEvent(int e) {
            for (int i = 0; i < eventNdx; i++) {
                if (events[i] == e) {
                    return i;
                }
            }
            return eventNdx;
        }

        private static final String[] names =
                {"UNKNOWN", "EV_CLEAR", "EV_CLEAN", "EV_UNREF", "EV_CLEAR_CLEANUP"};

        public String eventName(int event) {
            return names[event];
        }

        public synchronized String eventsString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = 0; i < eventNdx; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(eventName(events[i]));
            }
            sb.append(']');
            sb.append(", throwEx: ");
            sb.append(throwsEx);
            return sb.toString();
        }

        public String toString() {
            return String.format("Case: %s, expect: %s, events: %s",
                    getRef().getClass().getName(),
                    eventName(expectedResult()), eventsString());
        }
    }

    /**
     * Verify that casting a Cleanup to a Reference is not allowed to
     * get the referent or clear the reference.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void testReferentNotAvailable() {
        Cleaner cleaner = Cleaner.create();
        Semaphore s1 = new Semaphore(0);

        Object obj = new String("a new string");
        Cleaner.Cleanable c = cleaner.register(obj, () -> s1.release());
        Reference r = (Reference) c;
        try {
            Object o = r.get();
            System.out.printf("r: %s%n", Objects.toString(o));
            Assert.fail("should not be able to get the referent from Cleanable");
        } catch (UnsupportedOperationException uoe) {
            // expected
        }

        try {
            r.clear();
            Assert.fail("should not be able to clear the referent from Cleanable");
        } catch (UnsupportedOperationException uoe) {
            // expected
        }

        obj = null;
        checkCleaned(s1, true, "reference was cleaned:");
        cleaner = null;
    }

    /**
     * Test the Cleaner from the CleanerFactory.
     */
    @Test
    void testCleanerFactory() {
        Cleaner cleaner = CleanerFactory.cleaner();

        Object obj = new Object();
        CleanableCase s = setupPhantom(cleaner, obj);
        obj = null;
        checkCleaned(s.getSemaphore(), true,
                "Object was cleaned using CleanerFactor.cleaner():");
    }
}
