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
 * Written by Martin Buchholz and Doug Lea with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @modules java.base/java.util.concurrent:open
 * @summary Test Phaser phase integer overflow behavior
 */

import java.util.concurrent.Phaser;
import java.lang.reflect.Field;

public class PhaseOverflow {
    Field stateField;

    void checkState(Phaser phaser,
                    int phase, int parties, int unarrived) {
        equal(phase, phaser.getPhase());
        equal(parties, phaser.getRegisteredParties());
        equal(unarrived, phaser.getUnarrivedParties());
    }

    void test(String[] args) throws Throwable {
        stateField = Phaser.class.getDeclaredField("state");
        stateField.setAccessible(true);
        testLeaf();
        testTiered();
    }

    void testLeaf() throws Throwable {
        Phaser phaser = new Phaser();
        // this is extremely dependent on internal representation
        stateField.setLong(phaser, ((Integer.MAX_VALUE - 1L) << 32) | 1L);
        checkState(phaser, Integer.MAX_VALUE - 1, 0, 0);
        phaser.register();
        checkState(phaser, Integer.MAX_VALUE - 1, 1, 1);
        phaser.arrive();
        checkState(phaser, Integer.MAX_VALUE, 1, 1);
        phaser.arrive();
        checkState(phaser, 0, 1, 1);
        phaser.arrive();
        checkState(phaser, 1, 1, 1);
    }

    int phaseInc(int phase) { return (phase + 1) & Integer.MAX_VALUE; }

    void testTiered() throws Throwable {
        Phaser root = new Phaser();
        // this is extremely dependent on internal representation
        stateField.setLong(root, ((Integer.MAX_VALUE - 1L) << 32) | 1L);
        checkState(root, Integer.MAX_VALUE - 1, 0, 0);
        Phaser p1 = new Phaser(root, 1);
        checkState(root, Integer.MAX_VALUE - 1, 1, 1);
        checkState(p1, Integer.MAX_VALUE - 1, 1, 1);
        Phaser p2 = new Phaser(root, 2);
        checkState(root, Integer.MAX_VALUE - 1, 2, 2);
        checkState(p2, Integer.MAX_VALUE - 1, 2, 2);
        int ph = Integer.MAX_VALUE - 1;
        for (int k = 0; k < 5; k++) {
            checkState(root, ph, 2, 2);
            checkState(p1, ph, 1, 1);
            checkState(p2, ph, 2, 2);
            p1.arrive();
            checkState(root, ph, 2, 1);
            checkState(p1, ph, 1, 0);
            checkState(p2, ph, 2, 2);
            p2.arrive();
            checkState(root, ph, 2, 1);
            checkState(p1, ph, 1, 0);
            checkState(p2, ph, 2, 1);
            p2.arrive();
            ph = phaseInc(ph);
            checkState(root, ph, 2, 2);
            checkState(p1, ph, 1, 1);
            checkState(p2, ph, 2, 2);
        }
        equal(3, ph);
    }

    void xtestTiered() throws Throwable {
        Phaser root = new Phaser();
        stateField.setLong(root, ((Integer.MAX_VALUE - 1L) << 32) | 1L);
        checkState(root, Integer.MAX_VALUE - 1, 0, 0);
        Phaser p1 = new Phaser(root, 1);
        checkState(root, Integer.MAX_VALUE - 1, 1, 1);
        checkState(p1, Integer.MAX_VALUE - 1, 1, 1);
        Phaser p2 = new Phaser(root, 2);
        checkState(root, Integer.MAX_VALUE - 1, 2, 2);
        checkState(p2, Integer.MAX_VALUE - 1, 2, 2);
        int ph = Integer.MAX_VALUE - 1;
        for (int k = 0; k < 5; k++) {
            checkState(root, ph, 2, 2);
            checkState(p1, ph, 1, 1);
            checkState(p2, ph, 2, 2);
            p1.arrive();
            checkState(root, ph, 2, 1);
            checkState(p1, ph, 1, 0);
            checkState(p2, ph, 2, 2);
            p2.arrive();
            checkState(root, ph, 2, 1);
            checkState(p1, ph, 1, 0);
            checkState(p2, ph, 2, 1);
            p2.arrive();
            ph = phaseInc(ph);
            checkState(root, ph, 2, 2);
            checkState(p1, ph, 1, 1);
            checkState(p2, ph, 2, 2);
        }
        equal(3, ph);
    }

    //--------------------- Infrastructure ---------------------------
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        new PhaseOverflow().instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
