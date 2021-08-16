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

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AtomicBooleanTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(AtomicBooleanTest.class);
    }

    /**
     * constructor initializes to given value
     */
    public void testConstructor() {
        assertTrue(new AtomicBoolean(true).get());
        assertFalse(new AtomicBoolean(false).get());
    }

    /**
     * default constructed initializes to false
     */
    public void testConstructor2() {
        AtomicBoolean ai = new AtomicBoolean();
        assertFalse(ai.get());
    }

    /**
     * get returns the last value set
     */
    public void testGetSet() {
        AtomicBoolean ai = new AtomicBoolean(true);
        assertTrue(ai.get());
        ai.set(false);
        assertFalse(ai.get());
        ai.set(true);
        assertTrue(ai.get());
    }

    /**
     * get returns the last value lazySet in same thread
     */
    public void testGetLazySet() {
        AtomicBoolean ai = new AtomicBoolean(true);
        assertTrue(ai.get());
        ai.lazySet(false);
        assertFalse(ai.get());
        ai.lazySet(true);
        assertTrue(ai.get());
    }

    /**
     * compareAndSet succeeds in changing value if equal to expected else fails
     */
    public void testCompareAndSet() {
        AtomicBoolean ai = new AtomicBoolean(true);
        assertTrue(ai.compareAndSet(true, false));
        assertFalse(ai.get());
        assertTrue(ai.compareAndSet(false, false));
        assertFalse(ai.get());
        assertFalse(ai.compareAndSet(true, false));
        assertFalse(ai.get());
        assertTrue(ai.compareAndSet(false, true));
        assertTrue(ai.get());
    }

    /**
     * compareAndSet in one thread enables another waiting for value
     * to succeed
     */
    public void testCompareAndSetInMultipleThreads() throws Exception {
        final AtomicBoolean ai = new AtomicBoolean(true);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() {
                while (!ai.compareAndSet(false, true)) Thread.yield();
            }});

        t.start();
        assertTrue(ai.compareAndSet(true, false));
        t.join(LONG_DELAY_MS);
        assertFalse(t.isAlive());
    }

    /**
     * repeated weakCompareAndSet succeeds in changing value when equal
     * to expected
     */
    @SuppressWarnings("deprecation")
    public void testWeakCompareAndSet() {
        AtomicBoolean ai = new AtomicBoolean(true);
        do {} while (!ai.weakCompareAndSet(true, false));
        assertFalse(ai.get());
        do {} while (!ai.weakCompareAndSet(false, false));
        assertFalse(ai.get());
        do {} while (!ai.weakCompareAndSet(false, true));
        assertTrue(ai.get());
    }

    /**
     * getAndSet returns previous value and sets to given value
     */
    public void testGetAndSet() {
        AtomicBoolean ai = new AtomicBoolean();
        boolean[] booleans = { false, true };
        for (boolean before : booleans)
            for (boolean after : booleans) {
                ai.set(before);
                assertEquals(before, ai.getAndSet(after));
                assertEquals(after, ai.get());
            }
    }

    /**
     * a deserialized/reserialized atomic holds same value
     */
    public void testSerialization() throws Exception {
        AtomicBoolean x = new AtomicBoolean();
        AtomicBoolean y = serialClone(x);
        x.set(true);
        AtomicBoolean z = serialClone(x);
        assertTrue(x.get());
        assertFalse(y.get());
        assertTrue(z.get());
    }

    /**
     * toString returns current value.
     */
    public void testToString() {
        AtomicBoolean ai = new AtomicBoolean();
        assertEquals(Boolean.toString(false), ai.toString());
        ai.set(true);
        assertEquals(Boolean.toString(true), ai.toString());
    }

}
