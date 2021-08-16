/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include John Vint
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Test;
import junit.framework.TestSuite;

public class PhaserTest extends JSR166TestCase {

    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(PhaserTest.class);
    }

    private static final int maxParties = 65535;

    /** Checks state of unterminated phaser. */
    protected void assertState(Phaser phaser,
                               int phase, int parties, int unarrived) {
        assertEquals(phase, phaser.getPhase());
        assertEquals(parties, phaser.getRegisteredParties());
        assertEquals(unarrived, phaser.getUnarrivedParties());
        assertEquals(parties - unarrived, phaser.getArrivedParties());
        assertFalse(phaser.isTerminated());
    }

    /** Checks state of terminated phaser. */
    protected void assertTerminated(Phaser phaser, int maxPhase, int parties) {
        assertTrue(phaser.isTerminated());
        int expectedPhase = maxPhase + Integer.MIN_VALUE;
        assertEquals(expectedPhase, phaser.getPhase());
        assertEquals(parties, phaser.getRegisteredParties());
        assertEquals(expectedPhase, phaser.register());
        assertEquals(expectedPhase, phaser.arrive());
        assertEquals(expectedPhase, phaser.arriveAndDeregister());
    }

    protected void assertTerminated(Phaser phaser, int maxPhase) {
        assertTerminated(phaser, maxPhase, 0);
    }

    /**
     * Empty constructor builds a new Phaser with no parent, no registered
     * parties and initial phase number of 0
     */
    public void testConstructorDefaultValues() {
        Phaser phaser = new Phaser();
        assertNull(phaser.getParent());
        assertEquals(0, phaser.getRegisteredParties());
        assertEquals(0, phaser.getArrivedParties());
        assertEquals(0, phaser.getUnarrivedParties());
        assertEquals(0, phaser.getPhase());
    }

    /**
     * Constructing with a negative number of parties throws
     * IllegalArgumentException
     */
    public void testConstructorNegativeParties() {
        try {
            new Phaser(-1);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructing with a negative number of parties throws
     * IllegalArgumentException
     */
    public void testConstructorNegativeParties2() {
        try {
            new Phaser(new Phaser(), -1);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructing with a number of parties > 65535 throws
     * IllegalArgumentException
     */
    public void testConstructorPartiesExceedsLimit() {
        new Phaser(maxParties);
        try {
            new Phaser(maxParties + 1);
            shouldThrow();
        } catch (IllegalArgumentException success) {}

        new Phaser(new Phaser(), maxParties);
        try {
            new Phaser(new Phaser(), maxParties + 1);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * The parent provided to the constructor should be returned from
     * a later call to getParent
     */
    public void testConstructor3() {
        Phaser parent = new Phaser();
        assertSame(parent, new Phaser(parent).getParent());
        assertNull(new Phaser(null).getParent());
    }

    /**
     * The parent being input into the parameter should equal the original
     * parent when being returned
     */
    public void testConstructor5() {
        Phaser parent = new Phaser();
        assertSame(parent, new Phaser(parent, 0).getParent());
        assertNull(new Phaser(null, 0).getParent());
    }

    /**
     * register() will increment the number of unarrived parties by
     * one and not affect its arrived parties
     */
    public void testRegister1() {
        Phaser phaser = new Phaser();
        assertState(phaser, 0, 0, 0);
        assertEquals(0, phaser.register());
        assertState(phaser, 0, 1, 1);
    }

    /**
     * Registering more than 65536 parties causes IllegalStateException
     */
    public void testRegister2() {
        Phaser phaser = new Phaser(0);
        assertState(phaser, 0, 0, 0);
        assertEquals(0, phaser.bulkRegister(maxParties - 10));
        assertState(phaser, 0, maxParties - 10, maxParties - 10);
        for (int i = 0; i < 10; i++) {
            assertState(phaser, 0, maxParties - 10 + i, maxParties - 10 + i);
            assertEquals(0, phaser.register());
        }
        assertState(phaser, 0, maxParties, maxParties);
        try {
            phaser.register();
            shouldThrow();
        } catch (IllegalStateException success) {}

        try {
            phaser.bulkRegister(Integer.MAX_VALUE);
            shouldThrow();
        } catch (IllegalStateException success) {}

        assertEquals(0, phaser.bulkRegister(0));
        assertState(phaser, 0, maxParties, maxParties);
    }

    /**
     * register() correctly returns the current barrier phase number
     * when invoked
     */
    public void testRegister3() {
        Phaser phaser = new Phaser();
        assertEquals(0, phaser.register());
        assertEquals(0, phaser.arrive());
        assertEquals(1, phaser.register());
        assertState(phaser, 1, 2, 2);
    }

    /**
     * register causes the next arrive to not increment the phase
     * rather retain the phase number
     */
    public void testRegister4() {
        Phaser phaser = new Phaser(1);
        assertEquals(0, phaser.arrive());
        assertEquals(1, phaser.register());
        assertEquals(1, phaser.arrive());
        assertState(phaser, 1, 2, 1);
    }

    /**
     * register on a subphaser that is currently empty succeeds, even
     * in the presence of another non-empty subphaser
     */
    public void testRegisterEmptySubPhaser() {
        Phaser root = new Phaser();
        Phaser child1 = new Phaser(root, 1);
        Phaser child2 = new Phaser(root, 0);
        assertEquals(0, child2.register());
        assertState(root, 0, 2, 2);
        assertState(child1, 0, 1, 1);
        assertState(child2, 0, 1, 1);
        assertEquals(0, child2.arriveAndDeregister());
        assertState(root, 0, 1, 1);
        assertState(child1, 0, 1, 1);
        assertState(child2, 0, 0, 0);
        assertEquals(0, child2.register());
        assertEquals(0, child2.arriveAndDeregister());
        assertState(root, 0, 1, 1);
        assertState(child1, 0, 1, 1);
        assertState(child2, 0, 0, 0);
        assertEquals(0, child1.arriveAndDeregister());
        assertTerminated(root, 1);
        assertTerminated(child1, 1);
        assertTerminated(child2, 1);
    }

    /**
     * Invoking bulkRegister with a negative parameter throws an
     * IllegalArgumentException
     */
    public void testBulkRegister1() {
        try {
            new Phaser().bulkRegister(-1);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * bulkRegister should correctly record the number of unarrived
     * parties with the number of parties being registered
     */
    public void testBulkRegister2() {
        Phaser phaser = new Phaser();
        assertEquals(0, phaser.bulkRegister(0));
        assertState(phaser, 0, 0, 0);
        assertEquals(0, phaser.bulkRegister(20));
        assertState(phaser, 0, 20, 20);
    }

    /**
     * Registering with a number of parties greater than or equal to 1<<16
     * throws IllegalStateException.
     */
    public void testBulkRegister3() {
        assertEquals(0, new Phaser().bulkRegister((1 << 16) - 1));

        try {
            new Phaser().bulkRegister(1 << 16);
            shouldThrow();
        } catch (IllegalStateException success) {}

        try {
            new Phaser(2).bulkRegister((1 << 16) - 2);
            shouldThrow();
        } catch (IllegalStateException success) {}
    }

    /**
     * the phase number increments correctly when tripping the barrier
     */
    public void testPhaseIncrement1() {
        for (int size = 1; size < 9; size++) {
            final Phaser phaser = new Phaser(size);
            for (int index = 0; index <= (1 << size); index++) {
                int phase = phaser.arrive();
                assertTrue(index % size == 0 ? (index / size) == phase : index - (phase * size) > 0);
            }
        }
    }

    /**
     * arrive() on a registered phaser increments phase.
     */
    public void testArrive1() {
        Phaser phaser = new Phaser(1);
        assertState(phaser, 0, 1, 1);
        assertEquals(0, phaser.arrive());
        assertState(phaser, 1, 1, 1);
    }

    /**
     * arriveAndDeregister does not wait for others to arrive at barrier
     */
    public void testArriveAndDeregister() {
        final Phaser phaser = new Phaser(1);
        for (int i = 0; i < 10; i++) {
            assertState(phaser, 0, 1, 1);
            assertEquals(0, phaser.register());
            assertState(phaser, 0, 2, 2);
            assertEquals(0, phaser.arriveAndDeregister());
            assertState(phaser, 0, 1, 1);
        }
        assertEquals(0, phaser.arriveAndDeregister());
        assertTerminated(phaser, 1);
    }

    /**
     * arriveAndDeregister does not wait for others to arrive at barrier
     */
    public void testArrive2() {
        final Phaser phaser = new Phaser();
        assertEquals(0, phaser.register());
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            assertEquals(0, phaser.register());
            threads.add(newStartedThread(new CheckedRunnable() {
                public void realRun() {
                    assertEquals(0, phaser.arriveAndDeregister());
                }}));
        }

        for (Thread thread : threads)
            awaitTermination(thread);
        assertState(phaser, 0, 1, 1);
        assertEquals(0, phaser.arrive());
        assertState(phaser, 1, 1, 1);
    }

    /**
     * arrive() returns a negative number if the Phaser is terminated
     */
    public void testArrive3() {
        Phaser phaser = new Phaser(1);
        phaser.forceTermination();
        assertTerminated(phaser, 0, 1);
        assertEquals(0, phaser.getPhase() + Integer.MIN_VALUE);
        assertTrue(phaser.arrive() < 0);
        assertTrue(phaser.register() < 0);
        assertTrue(phaser.arriveAndDeregister() < 0);
        assertTrue(phaser.awaitAdvance(1) < 0);
        assertTrue(phaser.getPhase() < 0);
    }

    /**
     * arriveAndDeregister() throws IllegalStateException if number of
     * registered or unarrived parties would become negative
     */
    public void testArriveAndDeregister1() {
        Phaser phaser = new Phaser();
        try {
            phaser.arriveAndDeregister();
            shouldThrow();
        } catch (IllegalStateException success) {}
    }

    /**
     * arriveAndDeregister reduces the number of arrived parties
     */
    public void testArriveAndDeregister2() {
        final Phaser phaser = new Phaser(1);
        assertEquals(0, phaser.register());
        assertEquals(0, phaser.arrive());
        assertState(phaser, 0, 2, 1);
        assertEquals(0, phaser.arriveAndDeregister());
        assertState(phaser, 1, 1, 1);
    }

    /**
     * arriveAndDeregister arrives at the barrier on a phaser with a parent and
     * when a deregistration occurs and causes the phaser to have zero parties
     * its parent will be deregistered as well
     */
    public void testArriveAndDeregister3() {
        Phaser parent = new Phaser();
        Phaser child = new Phaser(parent);
        assertState(child, 0, 0, 0);
        assertState(parent, 0, 0, 0);
        assertEquals(0, child.register());
        assertState(child, 0, 1, 1);
        assertState(parent, 0, 1, 1);
        assertEquals(0, child.arriveAndDeregister());
        assertTerminated(child, 1);
        assertTerminated(parent, 1);
    }

    /**
     * arriveAndDeregister deregisters one party from its parent when
     * the number of parties of child is zero after deregistration
     */
    public void testArriveAndDeregister4() {
        Phaser parent = new Phaser();
        Phaser child = new Phaser(parent);
        assertEquals(0, parent.register());
        assertEquals(0, child.register());
        assertState(child, 0, 1, 1);
        assertState(parent, 0, 2, 2);
        assertEquals(0, child.arriveAndDeregister());
        assertState(child, 0, 0, 0);
        assertState(parent, 0, 1, 1);
    }

    /**
     * arriveAndDeregister deregisters one party from its parent when
     * the number of parties of root is nonzero after deregistration.
     */
    public void testArriveAndDeregister5() {
        Phaser root = new Phaser();
        Phaser parent = new Phaser(root);
        Phaser child = new Phaser(parent);
        assertState(root, 0, 0, 0);
        assertState(parent, 0, 0, 0);
        assertState(child, 0, 0, 0);
        assertEquals(0, child.register());
        assertState(root, 0, 1, 1);
        assertState(parent, 0, 1, 1);
        assertState(child, 0, 1, 1);
        assertEquals(0, child.arriveAndDeregister());
        assertTerminated(child, 1);
        assertTerminated(parent, 1);
        assertTerminated(root, 1);
    }

    /**
     * arriveAndDeregister returns the phase in which it leaves the
     * phaser in after deregistration
     */
    public void testArriveAndDeregister6() {
        final Phaser phaser = new Phaser(2);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertEquals(0, phaser.arrive());
            }});
        assertEquals(1, phaser.arriveAndAwaitAdvance());
        assertState(phaser, 1, 2, 2);
        assertEquals(1, phaser.arriveAndDeregister());
        assertState(phaser, 1, 1, 1);
        assertEquals(1, phaser.arriveAndDeregister());
        assertTerminated(phaser, 2);
        awaitTermination(t);
    }

    /**
     * awaitAdvance succeeds upon advance
     */
    public void testAwaitAdvance1() {
        final Phaser phaser = new Phaser(1);
        assertEquals(0, phaser.arrive());
        assertEquals(1, phaser.awaitAdvance(0));
    }

    /**
     * awaitAdvance with a negative parameter will return without affecting the
     * phaser
     */
    public void testAwaitAdvance2() {
        Phaser phaser = new Phaser();
        assertTrue(phaser.awaitAdvance(-1) < 0);
        assertState(phaser, 0, 0, 0);
    }

    /**
     * awaitAdvanceInterruptibly blocks interruptibly
     */
    public void testAwaitAdvanceInterruptibly_Interruptible() throws InterruptedException {
        final Phaser phaser = new Phaser(1);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(2);

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                Thread.currentThread().interrupt();
                try {
                    phaser.awaitAdvanceInterruptibly(0);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    phaser.awaitAdvanceInterruptibly(0);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws TimeoutException {
                Thread.currentThread().interrupt();
                try {
                    phaser.awaitAdvanceInterruptibly(0, randomTimeout(), randomTimeUnit());
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    phaser.awaitAdvanceInterruptibly(0, LONGER_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        assertState(phaser, 0, 1, 1);
        if (randomBoolean()) assertThreadBlocks(t1, Thread.State.WAITING);
        if (randomBoolean()) assertThreadBlocks(t2, Thread.State.TIMED_WAITING);
        t1.interrupt();
        t2.interrupt();
        awaitTermination(t1);
        awaitTermination(t2);
        assertState(phaser, 0, 1, 1);
        assertEquals(0, phaser.arrive());
        assertState(phaser, 1, 1, 1);
    }

    /**
     * awaitAdvance continues waiting if interrupted before waiting
     */
    public void testAwaitAdvanceAfterInterrupt() {
        final Phaser phaser = new Phaser();
        assertEquals(0, phaser.register());
        final CountDownLatch pleaseArrive = new CountDownLatch(1);

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                Thread.currentThread().interrupt();
                assertEquals(0, phaser.register());
                assertEquals(0, phaser.arrive());
                pleaseArrive.countDown();
                assertTrue(Thread.currentThread().isInterrupted());
                assertEquals(1, phaser.awaitAdvance(0));
                assertTrue(Thread.interrupted());
            }});

        await(pleaseArrive);
        assertThreadBlocks(t, Thread.State.WAITING);
        assertEquals(0, phaser.arrive());
        awaitTermination(t);

        Thread.currentThread().interrupt();
        assertEquals(1, phaser.awaitAdvance(0));
        assertTrue(Thread.interrupted());
    }

    /**
     *  awaitAdvance continues waiting if interrupted while waiting
     */
    public void testAwaitAdvanceBeforeInterrupt() {
        final Phaser phaser = new Phaser();
        assertEquals(0, phaser.register());
        final CountDownLatch pleaseArrive = new CountDownLatch(1);

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertEquals(0, phaser.register());
                assertEquals(0, phaser.arrive());
                assertFalse(Thread.currentThread().isInterrupted());
                pleaseArrive.countDown();
                assertEquals(1, phaser.awaitAdvance(0));
                assertTrue(Thread.interrupted());
            }});

        await(pleaseArrive);
        assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        assertEquals(0, phaser.arrive());
        awaitTermination(t);

        Thread.currentThread().interrupt();
        assertEquals(1, phaser.awaitAdvance(0));
        assertTrue(Thread.interrupted());
    }

    /**
     * arriveAndAwaitAdvance continues waiting if interrupted before waiting
     */
    public void testArriveAndAwaitAdvanceAfterInterrupt() {
        final Phaser phaser = new Phaser();
        assertEquals(0, phaser.register());
        final CountDownLatch pleaseArrive = new CountDownLatch(1);

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                Thread.currentThread().interrupt();
                assertEquals(0, phaser.register());
                pleaseArrive.countDown();
                assertTrue(Thread.currentThread().isInterrupted());
                assertEquals(1, phaser.arriveAndAwaitAdvance());
                assertTrue(Thread.interrupted());
            }});

        await(pleaseArrive);
        assertThreadBlocks(t, Thread.State.WAITING);
        Thread.currentThread().interrupt();
        assertEquals(1, phaser.arriveAndAwaitAdvance());
        assertTrue(Thread.interrupted());
        awaitTermination(t);
    }

    /**
     * arriveAndAwaitAdvance continues waiting if interrupted while waiting
     */
    public void testArriveAndAwaitAdvanceBeforeInterrupt() {
        final Phaser phaser = new Phaser();
        assertEquals(0, phaser.register());
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertEquals(0, phaser.register());
                assertFalse(Thread.currentThread().isInterrupted());
                pleaseInterrupt.countDown();
                assertEquals(1, phaser.arriveAndAwaitAdvance());
                assertTrue(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        Thread.currentThread().interrupt();
        assertEquals(1, phaser.arriveAndAwaitAdvance());
        assertTrue(Thread.interrupted());
        awaitTermination(t);
    }

    /**
     * awaitAdvance atomically waits for all parties within the same phase to
     * complete before continuing
     */
    public void testAwaitAdvance4() {
        final Phaser phaser = new Phaser(4);
        final AtomicInteger count = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            threads.add(newStartedThread(new CheckedRunnable() {
                public void realRun() {
                    for (int k = 0; k < 3; k++) {
                        assertEquals(2 * k + 1, phaser.arriveAndAwaitAdvance());
                        count.incrementAndGet();
                        assertEquals(2 * k + 1, phaser.arrive());
                        assertEquals(2 * k + 2, phaser.awaitAdvance(2 * k + 1));
                        assertEquals(4 * (k + 1), count.get());
                    }}}));

        for (Thread thread : threads)
            awaitTermination(thread);
    }

    /**
     * awaitAdvance returns the current phase
     */
    public void testAwaitAdvance5() {
        final Phaser phaser = new Phaser(1);
        assertEquals(1, phaser.awaitAdvance(phaser.arrive()));
        assertEquals(1, phaser.getPhase());
        assertEquals(1, phaser.register());
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean goesFirst = ((i & 1) == 0);
            threads.add(newStartedThread(new CheckedRunnable() {
                public void realRun() {
                    if (goesFirst)
                        latch.countDown();
                    else
                        await(latch);
                    phaser.arrive();
                }}));
            if (goesFirst)
                await(latch);
            else
                latch.countDown();
            assertEquals(i + 2, phaser.awaitAdvance(phaser.arrive()));
            assertEquals(i + 2, phaser.getPhase());
        }
        for (Thread thread : threads)
            awaitTermination(thread);
    }

    /**
     * awaitAdvance returns the current phase in child phasers
     */
    public void testAwaitAdvanceTieredPhaser() throws Exception {
        final Phaser parent = new Phaser();
        final List<Phaser> zeroPartyChildren = new ArrayList<>(3);
        final List<Phaser> onePartyChildren = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            zeroPartyChildren.add(new Phaser(parent, 0));
            onePartyChildren.add(new Phaser(parent, 1));
        }
        final List<Phaser> phasers = new ArrayList<>();
        phasers.addAll(zeroPartyChildren);
        phasers.addAll(onePartyChildren);
        phasers.add(parent);
        for (Phaser phaser : phasers) {
            assertEquals(-42, phaser.awaitAdvance(-42));
            assertEquals(-42, phaser.awaitAdvanceInterruptibly(-42));
            assertEquals(-42, phaser.awaitAdvanceInterruptibly(-42, MEDIUM_DELAY_MS, MILLISECONDS));
        }

        for (Phaser child : onePartyChildren)
            assertEquals(0, child.arrive());
        for (Phaser phaser : phasers) {
            assertEquals(-42, phaser.awaitAdvance(-42));
            assertEquals(-42, phaser.awaitAdvanceInterruptibly(-42));
            assertEquals(-42, phaser.awaitAdvanceInterruptibly(-42, MEDIUM_DELAY_MS, MILLISECONDS));
            assertEquals(1, phaser.awaitAdvance(0));
            assertEquals(1, phaser.awaitAdvanceInterruptibly(0));
            assertEquals(1, phaser.awaitAdvanceInterruptibly(0, MEDIUM_DELAY_MS, MILLISECONDS));
        }

        for (Phaser child : onePartyChildren)
            assertEquals(1, child.arrive());
        for (Phaser phaser : phasers) {
            assertEquals(-42, phaser.awaitAdvance(-42));
            assertEquals(-42, phaser.awaitAdvanceInterruptibly(-42));
            assertEquals(-42, phaser.awaitAdvanceInterruptibly(-42, MEDIUM_DELAY_MS, MILLISECONDS));
            assertEquals(2, phaser.awaitAdvance(0));
            assertEquals(2, phaser.awaitAdvanceInterruptibly(0));
            assertEquals(2, phaser.awaitAdvanceInterruptibly(0, MEDIUM_DELAY_MS, MILLISECONDS));
            assertEquals(2, phaser.awaitAdvance(1));
            assertEquals(2, phaser.awaitAdvanceInterruptibly(1));
            assertEquals(2, phaser.awaitAdvanceInterruptibly(1, MEDIUM_DELAY_MS, MILLISECONDS));
        }
    }

    /**
     * awaitAdvance returns when the phaser is externally terminated
     */
    public void testAwaitAdvance6() {
        final Phaser phaser = new Phaser(3);
        final CountDownLatch pleaseForceTermination = new CountDownLatch(2);
        final List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Runnable r = new CheckedRunnable() {
                public void realRun() {
                    assertEquals(0, phaser.arrive());
                    pleaseForceTermination.countDown();
                    assertTrue(phaser.awaitAdvance(0) < 0);
                    assertTrue(phaser.isTerminated());
                    assertTrue(phaser.getPhase() < 0);
                    assertEquals(0, phaser.getPhase() + Integer.MIN_VALUE);
                    assertEquals(3, phaser.getRegisteredParties());
                }};
            threads.add(newStartedThread(r));
        }
        await(pleaseForceTermination);
        phaser.forceTermination();
        assertTrue(phaser.isTerminated());
        assertEquals(0, phaser.getPhase() + Integer.MIN_VALUE);
        for (Thread thread : threads)
            awaitTermination(thread);
        assertEquals(3, phaser.getRegisteredParties());
    }

    /**
     * arriveAndAwaitAdvance throws IllegalStateException with no
     * unarrived parties
     */
    public void testArriveAndAwaitAdvance1() {
        Phaser phaser = new Phaser();
        try {
            phaser.arriveAndAwaitAdvance();
            shouldThrow();
        } catch (IllegalStateException success) {}
    }

    /**
     * arriveAndAwaitAdvance waits for all threads to arrive, the
     * number of arrived parties is the same number that is accounted
     * for when the main thread awaitsAdvance
     */
    public void testArriveAndAwaitAdvance3() {
        final Phaser phaser = new Phaser(1);
        final int THREADS = 3;
        final CountDownLatch pleaseArrive = new CountDownLatch(THREADS);
        final List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREADS; i++)
            threads.add(newStartedThread(new CheckedRunnable() {
                public void realRun() {
                    assertEquals(0, phaser.register());
                    pleaseArrive.countDown();
                    assertEquals(1, phaser.arriveAndAwaitAdvance());
                }}));

        await(pleaseArrive);
        long startTime = System.nanoTime();
        while (phaser.getArrivedParties() < THREADS)
            Thread.yield();
        assertEquals(THREADS, phaser.getArrivedParties());
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        for (Thread thread : threads)
            assertThreadBlocks(thread, Thread.State.WAITING);
        for (Thread thread : threads)
            assertTrue(thread.isAlive());
        assertState(phaser, 0, THREADS + 1, 1);
        phaser.arriveAndAwaitAdvance();
        for (Thread thread : threads)
            awaitTermination(thread);
        assertState(phaser, 1, THREADS + 1, THREADS + 1);
    }

}
