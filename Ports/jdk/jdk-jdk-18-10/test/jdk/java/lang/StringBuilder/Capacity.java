/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8149330
 * @summary Basic set of tests of capacity management
 * @run testng Capacity
 */

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.*;

public class Capacity {
    static final int DEFAULT_CAPACITY = 16;

    private static int newCapacity(int oldCapacity,
            int desiredCapacity)
    {
        return Math.max(oldCapacity * 2 + 2, desiredCapacity);
    }

    private static int nextNewCapacity(int oldCapacity) {
        return newCapacity(oldCapacity, oldCapacity + 1);
    }

    @Test(dataProvider = "singleChar")
    public void defaultCapacity(Character ch) {
        StringBuilder sb = new StringBuilder();
        assertEquals(sb.capacity(), DEFAULT_CAPACITY);
        for (int i = 0; i < DEFAULT_CAPACITY; i++) {
            sb.append(ch);
            assertEquals(sb.capacity(), DEFAULT_CAPACITY);
        }
        sb.append(ch);
        assertEquals(sb.capacity(), nextNewCapacity(DEFAULT_CAPACITY));
    }

    @Test(dataProvider = "charCapacity")
    public void explicitCapacity(Character ch, int initCapacity) {
        StringBuilder sb = new StringBuilder(initCapacity);
        assertEquals(sb.capacity(), initCapacity);
        for (int i = 0; i < initCapacity; i++) {
            sb.append(ch);
            assertEquals(sb.capacity(), initCapacity);
        }
        sb.append(ch);
        assertEquals(sb.capacity(), nextNewCapacity(initCapacity));
    }

    @Test(dataProvider = "singleChar")
    public void sbFromString(Character ch) {
        String s = "string " + ch;
        int expectedCapacity = s.length() + DEFAULT_CAPACITY;
        StringBuilder sb = new StringBuilder(s);
        assertEquals(sb.capacity(), expectedCapacity);
        for (int i = 0; i < DEFAULT_CAPACITY; i++) {
            sb.append(ch);
            assertEquals(sb.capacity(), expectedCapacity);
        }
        sb.append(ch);
        assertEquals(sb.capacity(), nextNewCapacity(expectedCapacity));
    }

    @Test(dataProvider = "singleChar")
    public void sbFromCharSeq(Character ch) {
        CharSequence cs = new MyCharSeq("char seq " + ch);
        int expectedCapacity = cs.length() + DEFAULT_CAPACITY;
        StringBuilder sb = new StringBuilder(cs);
        assertEquals(sb.capacity(), expectedCapacity);
        for (int i = 0; i < DEFAULT_CAPACITY; i++) {
            sb.append(ch);
            assertEquals(sb.capacity(), expectedCapacity);
        }
        sb.append(ch);
        assertEquals(sb.capacity(), nextNewCapacity(expectedCapacity));
    }

    @Test(dataProvider = "charCapacity")
    public void ensureCapacity(Character ch, int cap) {
        StringBuilder sb = new StringBuilder(0);
        assertEquals(sb.capacity(), 0);
        sb.ensureCapacity(cap); // only has effect if cap > 0
        int newCap = (cap == 0) ? 0 : newCapacity(0, cap);
        assertEquals(sb.capacity(), newCap);
        sb.ensureCapacity(newCap + 1);
        assertEquals(sb.capacity(), nextNewCapacity(newCap));
        sb.append(ch);
        assertEquals(sb.capacity(), nextNewCapacity(newCap));
    }

    @Test(dataProvider = "negativeCapacity",
          expectedExceptions = NegativeArraySizeException.class)
    public void negativeInitialCapacity(int negCap) {
        StringBuilder sb = new StringBuilder(negCap);
    }

    @Test(dataProvider = "negativeCapacity")
    public void ensureNegativeCapacity(int negCap) {
        StringBuilder sb = new StringBuilder();
        sb.ensureCapacity(negCap);
        assertEquals(sb.capacity(), DEFAULT_CAPACITY);
    }

    @Test(dataProvider = "charCapacity")
    public void trimToSize(Character ch, int cap) {
        StringBuilder sb = new StringBuilder(cap);
        int halfOfCap = cap / 2;
        for (int i = 0; i < halfOfCap; i++) {
            sb.append(ch);
        }
        sb.trimToSize();
        // according to the spec, capacity doesn't have to
        // become exactly the size
        assertTrue(sb.capacity() >= halfOfCap);
    }

    @DataProvider
    public Object[][] singleChar() {
        return new Object[][] { {'J'}, {'\u042b'} };
    }

    @DataProvider
    public Object[][] charCapacity() {
        return new Object[][] {
            {'J', 0},
            {'J', 1},
            {'J', 15},
            {'J', DEFAULT_CAPACITY},
            {'J', 1024},
            {'\u042b', 0},
            {'\u042b', 1},
            {'\u042b', 15},
            {'\u042b', DEFAULT_CAPACITY},
            {'\u042b', 1024},
        };
    }

    @DataProvider
    public Object[][] negativeCapacity() {
        return new Object[][] { {-1}, {Integer.MIN_VALUE} };
    }

    private static class MyCharSeq implements CharSequence {
        private CharSequence s;
        public MyCharSeq(CharSequence s) { this.s = s; }
        public char charAt(int i) { return s.charAt(i); }
        public int length() { return s.length(); }
        public CharSequence subSequence(int st, int e) {
            return s.subSequence(st, e);
        }
        public String toString() { return s.toString(); }
    }
}
