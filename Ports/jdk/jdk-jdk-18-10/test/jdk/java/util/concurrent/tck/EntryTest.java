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
 */

import java.util.AbstractMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

public class EntryTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(EntryTest.class);
    }

    static final String k1 = "1";
    static final String v1 = "a";
    static final String k2 = "2";
    static final String v2 = "b";

    /**
     * A new SimpleEntry(k, v) holds k, v.
     */
    public void testConstructor1() {
        Map.Entry e = new AbstractMap.SimpleEntry(k1, v1);
        assertEquals(k1, e.getKey());
        assertEquals(v1, e.getValue());
    }

    /**
     * A new SimpleImmutableEntry(k, v) holds k, v.
     */
    public void testConstructor2() {
        Map.Entry s = new AbstractMap.SimpleImmutableEntry(k1, v1);
        assertEquals(k1, s.getKey());
        assertEquals(v1, s.getValue());
    }

    /**
     * A new SimpleEntry(entry(k, v)) holds k, v.
     */
    public void testConstructor3() {
        Map.Entry e2 = new AbstractMap.SimpleEntry(k1, v1);
        Map.Entry e = new AbstractMap.SimpleEntry(e2);
        assertEquals(k1, e.getKey());
        assertEquals(v1, e.getValue());
    }

    /**
     * A new SimpleImmutableEntry(entry(k, v)) holds k, v.
     */
    public void testConstructor4() {
        Map.Entry s2 = new AbstractMap.SimpleImmutableEntry(k1, v1);
        Map.Entry s = new AbstractMap.SimpleImmutableEntry(s2);
        assertEquals(k1, s.getKey());
        assertEquals(v1, s.getValue());
    }

    /**
     * Entries with same key-value pairs are equal and have same
     * hashcodes
     */
    public void testEquals() {
        Map.Entry e2 = new AbstractMap.SimpleEntry(k1, v1);
        Map.Entry e = new AbstractMap.SimpleEntry(e2);
        Map.Entry s2 = new AbstractMap.SimpleImmutableEntry(k1, v1);
        Map.Entry s = new AbstractMap.SimpleImmutableEntry(s2);
        assertEquals(e2, e);
        assertEquals(e2.hashCode(), e.hashCode());
        assertEquals(s2, s);
        assertEquals(s2.hashCode(), s.hashCode());
        assertEquals(e2, s2);
        assertEquals(e2.hashCode(), s2.hashCode());
        assertEquals(e, s);
        assertEquals(e.hashCode(), s.hashCode());
    }

    /**
     * Entries with different key-value pairs are not equal
     */
    public void testNotEquals() {
        Map.Entry e2 = new AbstractMap.SimpleEntry(k1, v1);
        Map.Entry e = new AbstractMap.SimpleEntry(k2, v1);
        assertFalse(e2.equals(e));
        e = new AbstractMap.SimpleEntry(k1, v2);
        assertFalse(e2.equals(e));
        e = new AbstractMap.SimpleEntry(k2, v2);
        assertFalse(e2.equals(e));

        Map.Entry s2 = new AbstractMap.SimpleImmutableEntry(k1, v1);
        Map.Entry s = new AbstractMap.SimpleImmutableEntry(k2, v1);
        assertFalse(s2.equals(s));
        s = new AbstractMap.SimpleImmutableEntry(k1, v2);
        assertFalse(s2.equals(s));
        s = new AbstractMap.SimpleImmutableEntry(k2, v2);
        assertFalse(s2.equals(s));
    }

    /**
     * getValue returns last setValue for SimpleEntry
     */
    public void testSetValue1() {
        Map.Entry e2 = new AbstractMap.SimpleEntry(k1, v1);
        Map.Entry e = new AbstractMap.SimpleEntry(e2);
        assertEquals(k1, e.getKey());
        assertEquals(v1, e.getValue());
        e.setValue(k2);
        assertEquals(k2, e.getValue());
        assertFalse(e2.equals(e));
    }

    /**
     * setValue for SimpleImmutableEntry throws UnsupportedOperationException
     */
    public void testSetValue2() {
        Map.Entry s2 = new AbstractMap.SimpleImmutableEntry(k1, v1);
        Map.Entry s = new AbstractMap.SimpleImmutableEntry(s2);
        assertEquals(k1, s.getKey());
        assertEquals(v1, s.getValue());
        try {
            s.setValue(k2);
            shouldThrow();
        } catch (UnsupportedOperationException success) {}
    }
}
