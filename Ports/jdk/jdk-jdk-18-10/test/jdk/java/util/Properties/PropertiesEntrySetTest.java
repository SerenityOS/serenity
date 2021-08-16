/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug        8245694
 * @summary    tests the entrySet() method of Properties class
 * @author     Yu Li
 * @run testng PropertiesEntrySetTest
 */

import org.testng.annotations.Test;

import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class PropertiesEntrySetTest {

    @Test
    public void testEquals() {
        Properties a = new Properties();
        var aEntrySet = a.entrySet();
        assertFalse(aEntrySet.equals(null));
        assertTrue(aEntrySet.equals(aEntrySet));

        Properties b = new Properties();
        var bEntrySet = b.entrySet();
        assertTrue(bEntrySet.equals(aEntrySet));
        assertTrue(bEntrySet.hashCode() == aEntrySet.hashCode());

        a.setProperty("p1", "1");
        assertFalse(bEntrySet.equals(aEntrySet));
        assertFalse(bEntrySet.hashCode() == aEntrySet.hashCode());

        b.setProperty("p1", "1");
        assertTrue(aEntrySet.equals(bEntrySet));
        assertTrue(bEntrySet.hashCode() == aEntrySet.hashCode());

        Properties c = new Properties();
        c.setProperty("p1", "2");
        var cEntrySet = c.entrySet();
        assertFalse(cEntrySet.equals(bEntrySet));
        assertFalse(bEntrySet.hashCode() == cEntrySet.hashCode());
        assertFalse(cEntrySet.equals(aEntrySet));
        assertFalse(aEntrySet.hashCode() == cEntrySet.hashCode());

        a.setProperty("p2", "2");
        Properties d = new Properties();
        d.setProperty("p2", "2");
        d.setProperty("p1", "1");
        var dEntrySet = d.entrySet();
        assertTrue(dEntrySet.equals(aEntrySet));
        assertTrue(aEntrySet.hashCode() == dEntrySet.hashCode());

        a.remove("p1");
        assertFalse(aEntrySet.equals(dEntrySet));
        assertFalse(aEntrySet.hashCode() == dEntrySet.hashCode());

        d.remove("p1", "1");
        assertTrue(dEntrySet.equals(aEntrySet));
        assertTrue(aEntrySet.hashCode() == dEntrySet.hashCode());

        a.clear();
        assertFalse(aEntrySet.equals(dEntrySet));
        assertFalse(aEntrySet.hashCode() == dEntrySet.hashCode());
        assertTrue(aEntrySet.isEmpty());

        d.clear();
        assertTrue(dEntrySet.equals(aEntrySet));
        assertTrue(aEntrySet.hashCode() == dEntrySet.hashCode());
        assertTrue(dEntrySet.isEmpty());
    }

    @Test
    public void testToString() {
        Properties a = new Properties();
        var aEntrySet = a.entrySet();
        assertEquals(aEntrySet.toString(), "[]");

        a.setProperty("p1", "1");
        assertEquals(aEntrySet.toString(), "[p1=1]");

        a.setProperty("p2", "2");
        assertEquals(aEntrySet.size(), 2);
        assertTrue(aEntrySet.toString().trim().startsWith("["));
        assertTrue(aEntrySet.toString().contains("p1=1"));
        assertTrue(aEntrySet.toString().contains("p2=2"));
        assertTrue(aEntrySet.toString().trim().endsWith("]"));

        Properties b = new Properties();
        b.setProperty("p2", "2");
        b.setProperty("p1", "1");
        var bEntrySet = b.entrySet();
        assertEquals(bEntrySet.size(), 2);
        assertTrue(bEntrySet.toString().trim().startsWith("["));
        assertTrue(bEntrySet.toString().contains("p1=1"));
        assertTrue(bEntrySet.toString().contains("p2=2"));
        assertTrue(bEntrySet.toString().trim().endsWith("]"));

        b.setProperty("p0", "0");
        assertEquals(bEntrySet.size(), 3);
        assertTrue(bEntrySet.toString().contains("p0=0"));

        b.remove("p1");
        assertEquals(bEntrySet.size(), 2);
        assertFalse(bEntrySet.toString().contains("p1=1"));
        assertTrue(bEntrySet.toString().trim().startsWith("["));
        assertTrue(bEntrySet.toString().contains("p0=0"));
        assertTrue(bEntrySet.toString().contains("p2=2"));
        assertTrue(bEntrySet.toString().trim().endsWith("]"));

        b.remove("p0", "0");
        assertEquals(bEntrySet.size(), 1);
        assertFalse(bEntrySet.toString().contains("p0=0"));
        assertTrue(bEntrySet.toString().trim().startsWith("["));
        assertTrue(bEntrySet.toString().contains("p2=2"));
        assertTrue(bEntrySet.toString().trim().endsWith("]"));

        b.clear();
        assertTrue(bEntrySet.isEmpty());
        assertTrue(bEntrySet.toString().equals("[]"));
    }

    @Test
    public void testEntrySetWithoutException() {
        Properties a = new Properties();
        a.setProperty("p1", "1");
        a.setProperty("p2", "2");
        var aEntrySet = a.entrySet();
        assertEquals(aEntrySet.size(), 2);

        var i = aEntrySet.iterator();
        var e1 = i.next();
        i.remove();
        assertFalse(aEntrySet.contains(e1));
        assertEquals(aEntrySet.size(), 1);

        var e2 = i.next();
        aEntrySet.remove(e2);
        assertFalse(aEntrySet.contains(e2));
        assertTrue(aEntrySet.isEmpty());

        a.setProperty("p1", "1");
        a.setProperty("p3", "3");
        Properties b = new Properties();
        b.setProperty("p2", "2");
        b.setProperty("p1", "1");
        var bEntrySet = b.entrySet();

        assertFalse(bEntrySet.containsAll(aEntrySet));
        assertEquals(bEntrySet.size(), 2);

        assertTrue(bEntrySet.removeAll(aEntrySet));
        assertEquals(bEntrySet.size(), 1);

        assertTrue(bEntrySet.retainAll(aEntrySet));
        assertTrue(bEntrySet.isEmpty());
        assertEquals(aEntrySet.size(), 2);

        aEntrySet.clear();
        assertTrue(aEntrySet.isEmpty());
    }

    @Test
    public void testEntrySetExceptionWhenAdd() {
        Properties a = new Properties();
        a.setProperty("p1", "1");
        var aEntrySet = a.entrySet();

        Properties b = new Properties();
        b.setProperty("p2", "2");
        var bEntrySet = b.entrySet();

        assertThrows(UnsupportedOperationException.class, () -> aEntrySet.addAll(bEntrySet));
        assertThrows(UnsupportedOperationException.class, () -> aEntrySet.add(bEntrySet.iterator().next()));
    }
}
