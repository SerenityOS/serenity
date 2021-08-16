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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests of atomic class methods accepting lambdas introduced in JDK8.
 */
public class Atomic8Test extends JSR166TestCase {

    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(Atomic8Test.class);
    }

    static long addLong17(long x) { return x + 17; }
    static int addInt17(int x) { return x + 17; }
    static Item addItem17(Item x) {
        return new Item(x.intValue() + 17);
    }
    static Item sumItem(Item x, Item y) {
        return new Item(x.intValue() + y.intValue());
    }

    volatile long aLongField;
    volatile int anIntField;
    volatile Item anItemField;

    AtomicLongFieldUpdater<Atomic8Test> aLongFieldUpdater() {
        return AtomicLongFieldUpdater.newUpdater
            (Atomic8Test.class, "aLongField");
    }

    AtomicIntegerFieldUpdater<Atomic8Test> anIntFieldUpdater() {
        return AtomicIntegerFieldUpdater.newUpdater
            (Atomic8Test.class, "anIntField");
    }

    AtomicReferenceFieldUpdater<Atomic8Test,Item> anItemFieldUpdater() {
        return AtomicReferenceFieldUpdater.newUpdater
            (Atomic8Test.class, Item.class, "anItemField");
    }

    /**
     * AtomicLong getAndUpdate returns previous value and updates
     * result of supplied function
     */
    public void testLongGetAndUpdate() {
        AtomicLong a = new AtomicLong(1L);
        mustEqual(1L, a.getAndUpdate(Atomic8Test::addLong17));
        mustEqual(18L, a.getAndUpdate(Atomic8Test::addLong17));
        mustEqual(35L, a.get());
    }

    /**
     * AtomicLong updateAndGet updates with supplied function and
     * returns result.
     */
    public void testLongUpdateAndGet() {
        AtomicLong a = new AtomicLong(1L);
        mustEqual(18L, a.updateAndGet(Atomic8Test::addLong17));
        mustEqual(35L, a.updateAndGet(Atomic8Test::addLong17));
    }

    /**
     * AtomicLong getAndAccumulate returns previous value and updates
     * with supplied function.
     */
    public void testLongGetAndAccumulate() {
        AtomicLong a = new AtomicLong(1L);
        mustEqual(1L, a.getAndAccumulate(2L, Long::sum));
        mustEqual(3L, a.getAndAccumulate(3L, Long::sum));
        mustEqual(6L, a.get());
    }

    /**
     * AtomicLong accumulateAndGet updates with supplied function and
     * returns result.
     */
    public void testLongAccumulateAndGet() {
        AtomicLong a = new AtomicLong(1L);
        mustEqual(7L, a.accumulateAndGet(6L, Long::sum));
        mustEqual(10L, a.accumulateAndGet(3L, Long::sum));
        mustEqual(10L, a.get());
    }

    /**
     * AtomicInteger getAndUpdate returns previous value and updates
     * result of supplied function
     */
    public void testIntGetAndUpdate() {
        AtomicInteger a = new AtomicInteger(1);
        mustEqual(1, a.getAndUpdate(Atomic8Test::addInt17));
        mustEqual(18, a.getAndUpdate(Atomic8Test::addInt17));
        mustEqual(35, a.get());
    }

    /**
     * AtomicInteger updateAndGet updates with supplied function and
     * returns result.
     */
    public void testIntUpdateAndGet() {
        AtomicInteger a = new AtomicInteger(1);
        mustEqual(18, a.updateAndGet(Atomic8Test::addInt17));
        mustEqual(35, a.updateAndGet(Atomic8Test::addInt17));
        mustEqual(35, a.get());
    }

    /**
     * AtomicInteger getAndAccumulate returns previous value and updates
     * with supplied function.
     */
    public void testIntGetAndAccumulate() {
        AtomicInteger a = new AtomicInteger(1);
        mustEqual(1, a.getAndAccumulate(2, Integer::sum));
        mustEqual(3, a.getAndAccumulate(3, Integer::sum));
        mustEqual(6, a.get());
    }

    /**
     * AtomicInteger accumulateAndGet updates with supplied function and
     * returns result.
     */
    public void testIntAccumulateAndGet() {
        AtomicInteger a = new AtomicInteger(1);
        mustEqual(7, a.accumulateAndGet(6, Integer::sum));
        mustEqual(10, a.accumulateAndGet(3, Integer::sum));
        mustEqual(10, a.get());
    }

    /**
     * AtomicReference getAndUpdate returns previous value and updates
     * result of supplied function
     */
    public void testReferenceGetAndUpdate() {
        AtomicReference<Item> a = new AtomicReference<>(one);
        mustEqual(1, a.getAndUpdate(Atomic8Test::addItem17));
        mustEqual(18, a.getAndUpdate(Atomic8Test::addItem17));
        mustEqual(35, a.get());
    }

    /**
     * AtomicReference updateAndGet updates with supplied function and
     * returns result.
     */
    public void testReferenceUpdateAndGet() {
        AtomicReference<Item> a = new AtomicReference<>(one);
        mustEqual(18, a.updateAndGet(Atomic8Test::addItem17));
        mustEqual(35, a.updateAndGet(Atomic8Test::addItem17));
        mustEqual(35, a.get());
    }

    /**
     * AtomicReference getAndAccumulate returns previous value and updates
     * with supplied function.
     */
    public void testReferenceGetAndAccumulate() {
        AtomicReference<Item> a = new AtomicReference<>(one);
        mustEqual( 1, a.getAndAccumulate(two, Atomic8Test::sumItem));
        mustEqual( 3, a.getAndAccumulate(three, Atomic8Test::sumItem));
        mustEqual( 6, a.get());
    }

    /**
     * AtomicReference accumulateAndGet updates with supplied function and
     * returns result.
     */
    public void testReferenceAccumulateAndGet() {
        AtomicReference<Item> a = new AtomicReference<>(one);
        mustEqual( 7, a.accumulateAndGet(six, Atomic8Test::sumItem));
        mustEqual( 10, a.accumulateAndGet(three, Atomic8Test::sumItem));
        mustEqual( 10, a.get());
    }

    /**
     * AtomicLongArray getAndUpdate returns previous value and updates
     * result of supplied function
     */
    public void testLongArrayGetAndUpdate() {
        AtomicLongArray a = new AtomicLongArray(1);
        a.set(0, 1);
        mustEqual(1L, a.getAndUpdate(0, Atomic8Test::addLong17));
        mustEqual(18L, a.getAndUpdate(0, Atomic8Test::addLong17));
        mustEqual(35L, a.get(0));
    }

    /**
     * AtomicLongArray updateAndGet updates with supplied function and
     * returns result.
     */
    public void testLongArrayUpdateAndGet() {
        AtomicLongArray a = new AtomicLongArray(1);
        a.set(0, 1);
        mustEqual(18L, a.updateAndGet(0, Atomic8Test::addLong17));
        mustEqual(35L, a.updateAndGet(0, Atomic8Test::addLong17));
        mustEqual(35L, a.get(0));
    }

    /**
     * AtomicLongArray getAndAccumulate returns previous value and updates
     * with supplied function.
     */
    public void testLongArrayGetAndAccumulate() {
        AtomicLongArray a = new AtomicLongArray(1);
        a.set(0, 1);
        mustEqual(1L, a.getAndAccumulate(0, 2L, Long::sum));
        mustEqual(3L, a.getAndAccumulate(0, 3L, Long::sum));
        mustEqual(6L, a.get(0));
    }

    /**
     * AtomicLongArray accumulateAndGet updates with supplied function and
     * returns result.
     */
    public void testLongArrayAccumulateAndGet() {
        AtomicLongArray a = new AtomicLongArray(1);
        a.set(0, 1);
        mustEqual(7L, a.accumulateAndGet(0, 6L, Long::sum));
        mustEqual(10L, a.accumulateAndGet(0, 3L, Long::sum));
        mustEqual(10L, a.get(0));
    }

    /**
     * AtomicIntegerArray getAndUpdate returns previous value and updates
     * result of supplied function
     */
    public void testIntArrayGetAndUpdate() {
        AtomicIntegerArray a = new AtomicIntegerArray(1);
        a.set(0, 1);
        mustEqual(1, a.getAndUpdate(0, Atomic8Test::addInt17));
        mustEqual(18, a.getAndUpdate(0, Atomic8Test::addInt17));
        mustEqual(35, a.get(0));
    }

    /**
     * AtomicIntegerArray updateAndGet updates with supplied function and
     * returns result.
     */
    public void testIntArrayUpdateAndGet() {
        AtomicIntegerArray a = new AtomicIntegerArray(1);
        a.set(0, 1);
        mustEqual(18, a.updateAndGet(0, Atomic8Test::addInt17));
        mustEqual(35, a.updateAndGet(0, Atomic8Test::addInt17));
        mustEqual(35, a.get(0));
    }

    /**
     * AtomicIntegerArray getAndAccumulate returns previous value and updates
     * with supplied function.
     */
    public void testIntArrayGetAndAccumulate() {
        AtomicIntegerArray a = new AtomicIntegerArray(1);
        a.set(0, 1);
        mustEqual(1, a.getAndAccumulate(0, 2, Integer::sum));
        mustEqual(3, a.getAndAccumulate(0, 3, Integer::sum));
        mustEqual(6, a.get(0));
    }

    /**
     * AtomicIntegerArray accumulateAndGet updates with supplied function and
     * returns result.
     */
    public void testIntArrayAccumulateAndGet() {
        AtomicIntegerArray a = new AtomicIntegerArray(1);
        a.set(0, 1);
        mustEqual(7, a.accumulateAndGet(0, 6, Integer::sum));
        mustEqual(10, a.accumulateAndGet(0, 3, Integer::sum));
    }

    /**
     * AtomicReferenceArray getAndUpdate returns previous value and updates
     * result of supplied function
     */
    public void testReferenceArrayGetAndUpdate() {
        AtomicReferenceArray<Item> a = new AtomicReferenceArray<>(1);
        a.set(0, one);
        mustEqual( 1, a.getAndUpdate(0, Atomic8Test::addItem17));
        mustEqual( 18, a.getAndUpdate(0, Atomic8Test::addItem17));
        mustEqual( 35, a.get(0));
    }

    /**
     * AtomicReferenceArray updateAndGet updates with supplied function and
     * returns result.
     */
    public void testReferenceArrayUpdateAndGet() {
        AtomicReferenceArray<Item> a = new AtomicReferenceArray<>(1);
        a.set(0, one);
        mustEqual( 18, a.updateAndGet(0, Atomic8Test::addItem17));
        mustEqual( 35, a.updateAndGet(0, Atomic8Test::addItem17));
    }

    /**
     * AtomicReferenceArray getAndAccumulate returns previous value and updates
     * with supplied function.
     */
    public void testReferenceArrayGetAndAccumulate() {
        AtomicReferenceArray<Item> a = new AtomicReferenceArray<>(1);
        a.set(0, one);
        mustEqual( 1, a.getAndAccumulate(0, two, Atomic8Test::sumItem));
        mustEqual( 3, a.getAndAccumulate(0, three, Atomic8Test::sumItem));
        mustEqual( 6, a.get(0));
    }

    /**
     * AtomicReferenceArray accumulateAndGet updates with supplied function and
     * returns result.
     */
    public void testReferenceArrayAccumulateAndGet() {
        AtomicReferenceArray<Item> a = new AtomicReferenceArray<>(1);
        a.set(0, one);
        mustEqual( 7, a.accumulateAndGet(0, six, Atomic8Test::sumItem));
        mustEqual( 10, a.accumulateAndGet(0, three, Atomic8Test::sumItem));
    }

    /**
     * AtomicLongFieldUpdater getAndUpdate returns previous value and updates
     * result of supplied function
     */
    public void testLongFieldUpdaterGetAndUpdate() {
        AtomicLongFieldUpdater<Atomic8Test> a = aLongFieldUpdater();
        a.set(this, 1L);
        mustEqual(1L, a.getAndUpdate(this, Atomic8Test::addLong17));
        mustEqual(18L, a.getAndUpdate(this, Atomic8Test::addLong17));
        mustEqual(35L, a.get(this));
        mustEqual(35L, aLongField);
    }

    /**
     * AtomicLongFieldUpdater updateAndGet updates with supplied function and
     * returns result.
     */
    public void testLongFieldUpdaterUpdateAndGet() {
        AtomicLongFieldUpdater<Atomic8Test> a = aLongFieldUpdater();
        a.set(this, 1L);
        mustEqual(18L, a.updateAndGet(this, Atomic8Test::addLong17));
        mustEqual(35L, a.updateAndGet(this, Atomic8Test::addLong17));
        mustEqual(35L, a.get(this));
        mustEqual(35L, aLongField);
    }

    /**
     * AtomicLongFieldUpdater getAndAccumulate returns previous value
     * and updates with supplied function.
     */
    public void testLongFieldUpdaterGetAndAccumulate() {
        AtomicLongFieldUpdater<Atomic8Test> a = aLongFieldUpdater();
        a.set(this, 1L);
        mustEqual(1L, a.getAndAccumulate(this, 2L, Long::sum));
        mustEqual(3L, a.getAndAccumulate(this, 3L, Long::sum));
        mustEqual(6L, a.get(this));
        mustEqual(6L, aLongField);
    }

    /**
     * AtomicLongFieldUpdater accumulateAndGet updates with supplied
     * function and returns result.
     */
    public void testLongFieldUpdaterAccumulateAndGet() {
        AtomicLongFieldUpdater<Atomic8Test> a = aLongFieldUpdater();
        a.set(this, 1L);
        mustEqual(7L, a.accumulateAndGet(this, 6L, Long::sum));
        mustEqual(10L, a.accumulateAndGet(this, 3L, Long::sum));
        mustEqual(10L, a.get(this));
        mustEqual(10L, aLongField);
    }

    /**
     * AtomicIntegerFieldUpdater getAndUpdate returns previous value and updates
     * result of supplied function
     */
    public void testIntegerFieldUpdaterGetAndUpdate() {
        AtomicIntegerFieldUpdater<Atomic8Test> a = anIntFieldUpdater();
        a.set(this, 1);
        mustEqual(1, a.getAndUpdate(this, Atomic8Test::addInt17));
        mustEqual(18, a.getAndUpdate(this, Atomic8Test::addInt17));
        mustEqual(35, a.get(this));
        mustEqual(35, anIntField);
    }

    /**
     * AtomicIntegerFieldUpdater updateAndGet updates with supplied function and
     * returns result.
     */
    public void testIntegerFieldUpdaterUpdateAndGet() {
        AtomicIntegerFieldUpdater<Atomic8Test> a = anIntFieldUpdater();
        a.set(this, 1);
        mustEqual(18, a.updateAndGet(this, Atomic8Test::addInt17));
        mustEqual(35, a.updateAndGet(this, Atomic8Test::addInt17));
        mustEqual(35, a.get(this));
        mustEqual(35, anIntField);
    }

    /**
     * AtomicIntegerFieldUpdater getAndAccumulate returns previous value
     * and updates with supplied function.
     */
    public void testIntegerFieldUpdaterGetAndAccumulate() {
        AtomicIntegerFieldUpdater<Atomic8Test> a = anIntFieldUpdater();
        a.set(this, 1);
        mustEqual(1, a.getAndAccumulate(this, 2, Integer::sum));
        mustEqual(3, a.getAndAccumulate(this, 3, Integer::sum));
        mustEqual(6, a.get(this));
        mustEqual(6, anIntField);
    }

    /**
     * AtomicIntegerFieldUpdater accumulateAndGet updates with supplied
     * function and returns result.
     */
    public void testIntegerFieldUpdaterAccumulateAndGet() {
        AtomicIntegerFieldUpdater<Atomic8Test> a = anIntFieldUpdater();
        a.set(this, 1);
        mustEqual(7, a.accumulateAndGet(this, 6, Integer::sum));
        mustEqual(10, a.accumulateAndGet(this, 3, Integer::sum));
        mustEqual(10, a.get(this));
        mustEqual(10, anIntField);
    }

    /**
     * AtomicReferenceFieldUpdater getAndUpdate returns previous value
     * and updates result of supplied function
     */
    public void testReferenceFieldUpdaterGetAndUpdate() {
        AtomicReferenceFieldUpdater<Atomic8Test,Item> a = anItemFieldUpdater();
        a.set(this, one);
        mustEqual( 1, a.getAndUpdate(this, Atomic8Test::addItem17));
        mustEqual( 18, a.getAndUpdate(this, Atomic8Test::addItem17));
        mustEqual( 35, a.get(this));
        mustEqual( 35, anItemField);
    }

    /**
     * AtomicReferenceFieldUpdater updateAndGet updates with supplied
     * function and returns result.
     */
    public void testReferenceFieldUpdaterUpdateAndGet() {
        AtomicReferenceFieldUpdater<Atomic8Test,Item> a = anItemFieldUpdater();
        a.set(this, one);
        mustEqual( 18, a.updateAndGet(this, Atomic8Test::addItem17));
        mustEqual( 35, a.updateAndGet(this, Atomic8Test::addItem17));
        mustEqual( 35, a.get(this));
        mustEqual( 35, anItemField);
    }

    /**
     * AtomicReferenceFieldUpdater returns previous value and updates
     * with supplied function.
     */
    public void testReferenceFieldUpdaterGetAndAccumulate() {
        AtomicReferenceFieldUpdater<Atomic8Test,Item> a = anItemFieldUpdater();
        a.set(this, one);
        mustEqual( 1, a.getAndAccumulate(this, two, Atomic8Test::sumItem));
        mustEqual( 3, a.getAndAccumulate(this, three, Atomic8Test::sumItem));
        mustEqual( 6, a.get(this));
        mustEqual( 6, anItemField);
    }

    /**
     * AtomicReferenceFieldUpdater accumulateAndGet updates with
     * supplied function and returns result.
     */
    public void testReferenceFieldUpdaterAccumulateAndGet() {
        AtomicReferenceFieldUpdater<Atomic8Test,Item> a = anItemFieldUpdater();
        a.set(this, one);
        mustEqual( 7, a.accumulateAndGet(this, six, Atomic8Test::sumItem));
        mustEqual( 10, a.accumulateAndGet(this, three, Atomic8Test::sumItem));
        mustEqual( 10, a.get(this));
        mustEqual( 10, anItemField);
    }

    /**
     * All Atomic getAndUpdate methods throw NullPointerException on
     * null function argument
     */
    @SuppressWarnings("unchecked")
    public void testGetAndUpdateNPE() {
        assertThrows(
            NullPointerException.class,
            () -> new AtomicLong().getAndUpdate(null),
            () -> new AtomicInteger().getAndUpdate(null),
            () -> new AtomicReference().getAndUpdate(null),
            () -> new AtomicLongArray(1).getAndUpdate(0, null),
            () -> new AtomicIntegerArray(1).getAndUpdate(0, null),
            () -> new AtomicReferenceArray(1).getAndUpdate(0, null),
            () -> aLongFieldUpdater().getAndUpdate(this, null),
            () -> anIntFieldUpdater().getAndUpdate(this, null),
            () -> anItemFieldUpdater().getAndUpdate(this, null));
    }

    /**
     * All Atomic updateAndGet methods throw NullPointerException on null function argument
     */
    @SuppressWarnings("unchecked")
    public void testUpdateAndGetNPE() {
        assertThrows(
            NullPointerException.class,
            () -> new AtomicLong().updateAndGet(null),
            () -> new AtomicInteger().updateAndGet(null),
            () -> new AtomicReference().updateAndGet(null),
            () -> new AtomicLongArray(1).updateAndGet(0, null),
            () -> new AtomicIntegerArray(1).updateAndGet(0, null),
            () -> new AtomicReferenceArray(1).updateAndGet(0, null),
            () -> aLongFieldUpdater().updateAndGet(this, null),
            () -> anIntFieldUpdater().updateAndGet(this, null),
            () -> anItemFieldUpdater().updateAndGet(this, null));
    }

    /**
     * All Atomic getAndAccumulate methods throw NullPointerException
     * on null function argument
     */
    @SuppressWarnings("unchecked")
    public void testGetAndAccumulateNPE() {
        assertThrows(
            NullPointerException.class,
            () -> new AtomicLong().getAndAccumulate(1L, null),
            () -> new AtomicInteger().getAndAccumulate(1, null),
            () -> new AtomicReference().getAndAccumulate(one, null),
            () -> new AtomicLongArray(1).getAndAccumulate(0, 1L, null),
            () -> new AtomicIntegerArray(1).getAndAccumulate(0, 1, null),
            () -> new AtomicReferenceArray(1).getAndAccumulate(0, one, null),
            () -> aLongFieldUpdater().getAndAccumulate(this, 1L, null),
            () -> anIntFieldUpdater().getAndAccumulate(this, 1, null),
            () -> anItemFieldUpdater().getAndAccumulate(this, one, null));
    }

    /**
     * All Atomic accumulateAndGet methods throw NullPointerException
     * on null function argument
     */
    @SuppressWarnings("unchecked")
    public void testAccumulateAndGetNPE() {
        assertThrows(
            NullPointerException.class,
            () -> new AtomicLong().accumulateAndGet(1L, null),
            () -> new AtomicInteger().accumulateAndGet(1, null),
            () -> new AtomicReference().accumulateAndGet(one, null),
            () -> new AtomicLongArray(1).accumulateAndGet(0, 1L, null),
            () -> new AtomicIntegerArray(1).accumulateAndGet(0, 1, null),
            () -> new AtomicReferenceArray(1).accumulateAndGet(0, one, null),
            () -> aLongFieldUpdater().accumulateAndGet(this, 1L, null),
            () -> anIntFieldUpdater().accumulateAndGet(this, 1, null),
            () -> anItemFieldUpdater().accumulateAndGet(this, one, null));
    }

    /**
     * Object arguments for parameters of type T that are not
     * instances of the class passed to the newUpdater call will
     * result in a ClassCastException being thrown.
     */
    @SuppressWarnings("unchecked")
    public void testFieldUpdaters_ClassCastException() {
        // Use raw types to allow passing wrong object type, provoking CCE
        final AtomicLongFieldUpdater longUpdater = aLongFieldUpdater();
        final AtomicIntegerFieldUpdater intUpdater = anIntFieldUpdater();
        final AtomicReferenceFieldUpdater refUpdater = anItemFieldUpdater();
        for (Object x : new Object[]{ new Object(), null }) {
            assertThrows(
                ClassCastException.class,
                () -> longUpdater.get(x),
                () -> intUpdater.get(x),
                () -> refUpdater.get(x),

                () -> longUpdater.set(x, 17L),
                () -> intUpdater.set(x, 17),
                () -> refUpdater.set(x,  new Item(17)),

                () -> longUpdater.addAndGet(x, 17L),
                () -> intUpdater.addAndGet(x, 17),

                () -> longUpdater.getAndUpdate(x, y -> y),
                () -> intUpdater.getAndUpdate(x, y -> y),
                () -> refUpdater.getAndUpdate(x, y -> y),

                () -> longUpdater.compareAndSet(x, 17L, 42L),
                () -> intUpdater.compareAndSet(x, 17, 42),
                () -> refUpdater.compareAndSet(x,  17,  fortytwo));
        }
    }

}
