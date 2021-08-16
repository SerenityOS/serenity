/*
 * Copyright 2010 Google Inc.  All Rights Reserved.
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
 * @test
 * @bug 6952330
 * @summary Test StringBuffer/StringBuilder capacity handling.
 * @key randomness
 */

import java.util.Random;

public class Capacity {
    void test(String[] args) throws Throwable {
        Random rnd = new Random();
        int[] sizes = { 0, 1, rnd.nextInt(10), rnd.nextInt(1000) };
        for (int size : sizes) {
            equal(16, new StringBuffer().capacity());
            equal(16, new StringBuilder().capacity());
            StringBuffer buff = new StringBuffer(size);
            StringBuilder bild = new StringBuilder(size);
            equal(size, buff.capacity());
            equal(size, bild.capacity());
            buff.ensureCapacity(size);
            bild.ensureCapacity(size);
            equal(size, buff.capacity());
            equal(size, bild.capacity());
            buff.ensureCapacity(size+1);
            bild.ensureCapacity(size+1);
            equal(size*2+2, buff.capacity());
            equal(size*2+2, bild.capacity());
            size = buff.capacity();
            buff.ensureCapacity(size*2+1);
            bild.ensureCapacity(size*2+1);
            equal(size*2+2, buff.capacity());
            equal(size*2+2, bild.capacity());
            size = buff.capacity();
            int newSize = size * 2 + 3;
            buff.ensureCapacity(newSize);
            bild.ensureCapacity(newSize);
            equal(newSize, buff.capacity());
            equal(newSize, bild.capacity());
            buff.ensureCapacity(0);
            bild.ensureCapacity(0);
            equal(newSize, buff.capacity());
            equal(newSize, bild.capacity());
        }
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
        new Capacity().instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
