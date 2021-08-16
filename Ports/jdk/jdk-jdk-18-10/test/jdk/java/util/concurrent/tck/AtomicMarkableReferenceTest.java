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
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

import java.util.concurrent.atomic.AtomicMarkableReference;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AtomicMarkableReferenceTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(AtomicMarkableReferenceTest.class);
    }

    /**
     * constructor initializes to given reference and mark
     */
    public void testConstructor() {
        AtomicMarkableReference<Item> ai = new AtomicMarkableReference<>(one, false);
        assertSame(one, ai.getReference());
        assertFalse(ai.isMarked());
        AtomicMarkableReference<Item> a2 = new AtomicMarkableReference<>(null, true);
        assertNull(a2.getReference());
        assertTrue(a2.isMarked());
    }

    /**
     * get returns the last values of reference and mark set
     */
    public void testGetSet() {
        boolean[] mark = new boolean[1];
        AtomicMarkableReference<Item> ai = new AtomicMarkableReference<>(one, false);
        assertSame(one, ai.getReference());
        assertFalse(ai.isMarked());
        assertSame(one, ai.get(mark));
        assertFalse(mark[0]);
        ai.set(two, false);
        assertSame(two, ai.getReference());
        assertFalse(ai.isMarked());
        assertSame(two, ai.get(mark));
        assertFalse(mark[0]);
        ai.set(one, true);
        assertSame(one, ai.getReference());
        assertTrue(ai.isMarked());
        assertSame(one, ai.get(mark));
        assertTrue(mark[0]);
    }

    /**
     * attemptMark succeeds in single thread
     */
    public void testAttemptMark() {
        boolean[] mark = new boolean[1];
        AtomicMarkableReference<Item> ai = new AtomicMarkableReference<>(one, false);
        assertFalse(ai.isMarked());
        assertTrue(ai.attemptMark(one, true));
        assertTrue(ai.isMarked());
        assertSame(one, ai.get(mark));
        assertTrue(mark[0]);
    }

    /**
     * compareAndSet succeeds in changing values if equal to expected reference
     * and mark else fails
     */
    public void testCompareAndSet() {
        boolean[] mark = new boolean[1];
        AtomicMarkableReference<Item> ai = new AtomicMarkableReference<>(one, false);
        assertSame(one, ai.get(mark));
        assertFalse(ai.isMarked());
        assertFalse(mark[0]);

        assertTrue(ai.compareAndSet(one, two, false, false));
        assertSame(two, ai.get(mark));
        assertFalse(mark[0]);

        assertTrue(ai.compareAndSet(two, minusThree, false, true));
        assertSame(minusThree, ai.get(mark));
        assertTrue(mark[0]);

        assertFalse(ai.compareAndSet(two, minusThree, true, true));
        assertSame(minusThree, ai.get(mark));
        assertTrue(mark[0]);
    }

    /**
     * compareAndSet in one thread enables another waiting for reference value
     * to succeed
     */
    public void testCompareAndSetInMultipleThreads() throws Exception {
        final AtomicMarkableReference<Item> ai = new AtomicMarkableReference<>(one, false);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() {
                while (!ai.compareAndSet(two, three, false, false))
                    Thread.yield();
            }});

        t.start();
        assertTrue(ai.compareAndSet(one, two, false, false));
        t.join(LONG_DELAY_MS);
        assertFalse(t.isAlive());
        assertSame(three, ai.getReference());
        assertFalse(ai.isMarked());
    }

    /**
     * compareAndSet in one thread enables another waiting for mark value
     * to succeed
     */
    public void testCompareAndSetInMultipleThreads2() throws Exception {
        final AtomicMarkableReference<Item> ai = new AtomicMarkableReference<>(one, false);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() {
                while (!ai.compareAndSet(one, one, true, false))
                    Thread.yield();
            }});

        t.start();
        assertTrue(ai.compareAndSet(one, one, false, true));
        t.join(LONG_DELAY_MS);
        assertFalse(t.isAlive());
        assertSame(one, ai.getReference());
        assertFalse(ai.isMarked());
    }

    /**
     * repeated weakCompareAndSet succeeds in changing values when equal
     * to expected
     */
    public void testWeakCompareAndSet() {
        boolean[] mark = new boolean[1];
        AtomicMarkableReference<Item> ai = new AtomicMarkableReference<>(one, false);
        assertSame(one, ai.get(mark));
        assertFalse(ai.isMarked());
        assertFalse(mark[0]);

        do {} while (!ai.weakCompareAndSet(one, two, false, false));
        assertSame(two, ai.get(mark));
        assertFalse(mark[0]);

        do {} while (!ai.weakCompareAndSet(two, minusThree, false, true));
        assertSame(minusThree, ai.get(mark));
        assertTrue(mark[0]);
    }

}
