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
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Spliterator;

import junit.framework.Test;

public class ArrayDeque8Test extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        return newTestSuite(ArrayDeque8Test.class);
    }

    /**
     * Spliterator.getComparator always throws IllegalStateException
     */
    public void testSpliterator_getComparator() {
        assertThrows(IllegalStateException.class,
                     () -> new ArrayDeque<Item>().spliterator().getComparator());
    }

    /**
     * Spliterator characteristics are as advertised
     */
    public void testSpliterator_characteristics() {
        ArrayDeque<Item> q = new ArrayDeque<>();
        Spliterator<Item> s = q.spliterator();
        int characteristics = s.characteristics();
        int required = Spliterator.NONNULL
            | Spliterator.ORDERED
            | Spliterator.SIZED
            | Spliterator.SUBSIZED;
        mustEqual(required, characteristics & required);
        assertTrue(s.hasCharacteristics(required));
        mustEqual(0, characteristics
                     & (Spliterator.CONCURRENT
                        | Spliterator.DISTINCT
                        | Spliterator.IMMUTABLE
                        | Spliterator.SORTED));
    }

    /**
     * Handle capacities near Integer.MAX_VALUE.
     * ant -Dvmoptions='-Xms28g -Xmx28g' -Djsr166.expensiveTests=true -Djsr166.tckTestClass=ArrayDeque8Test -Djsr166.methodFilter=testHugeCapacity tck
     */
    public void testHugeCapacity() {
        if (! (testImplementationDetails
               && expensiveTests
               && Runtime.getRuntime().maxMemory() > 24L * (1 << 30)))
            return;

        final Item e = fortytwo;
        final int maxArraySize = Integer.MAX_VALUE - 8;

        assertThrows(OutOfMemoryError.class,
                     () -> new ArrayDeque<Item>(Integer.MAX_VALUE));

        {
            ArrayDeque<Object> q = new ArrayDeque<>(maxArraySize - 1);
            mustEqual(0, q.size());
            assertTrue(q.isEmpty());
            q = null;
        }

        {
            ArrayDeque<Object> q = new ArrayDeque<>();
            assertTrue(q.addAll(Collections.nCopies(maxArraySize - 3, e)));
            mustEqual(e, q.peekFirst());
            mustEqual(e, q.peekLast());
            mustEqual(maxArraySize - 3, q.size());
            q.addFirst(zero);
            q.addLast(one);
            mustEqual(zero, q.peekFirst());
            mustEqual(one, q.peekLast());
            mustEqual(maxArraySize - 1, q.size());

            ArrayDeque<Object> smallish = new ArrayDeque<>(
                Collections.nCopies(Integer.MAX_VALUE - q.size() + 1, e));
            assertThrows(
                IllegalStateException.class,
                () -> q.addAll(q),
                () -> q.addAll(smallish),
                () -> smallish.addAll(q));
        }
    }

}
