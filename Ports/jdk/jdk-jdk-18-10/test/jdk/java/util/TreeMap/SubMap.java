/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4252490
 * @summary The firstKey and lastKey
 */

import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class SubMap {
    public static void main(String[] args) throws Exception {
        SortedMap m = new TreeMap();
        m.put(new Integer(1), new Integer(1));
        m.put(new Integer(2), new Integer(2));
        m.put(new Integer(3), new Integer(3));
        SortedMap m2 = m.subMap(new Integer(2), new Integer(2));

        boolean exc = false;
        try {
            m2.firstKey();
        } catch (NoSuchElementException e) {
            exc = true;
        }
        if (!exc)
            throw new Exception("first key");

        exc = false;
        try {
            m2.lastKey();
        } catch (NoSuchElementException e) {
            exc = true;
        }
        if (!exc)
            throw new Exception("last key");

        SortedMap m3 = m.subMap(new Integer(2), new Integer(3));
        if (!m3.firstKey().equals(new Integer(2)))
            throw new Exception("first key wrong");
        if (!m3.lastKey().equals(new Integer(2)))
            throw new Exception("last key wrong");

        SortedSet s = new TreeSet();
        s.add(new Integer(1));
        s.add(new Integer(2));
        s.add(new Integer(3));
        SortedSet s2 = s.subSet(new Integer(2), new Integer(2));

        exc = false;
        try {
            s2.first();
        } catch (NoSuchElementException e) {
            exc = true;
        }
        if (!exc)
            throw new Exception("first element");

        exc = false;
        try {
            s2.last();
        } catch (NoSuchElementException e) {
            exc = true;
        }
        if (!exc)
            throw new Exception("last element");

        SortedSet s3 = s.subSet(new Integer(2), new Integer(3));
        if (!s3.first().equals(new Integer(2)))
            throw new Exception("first element wrong");
        if (!s3.last().equals(new Integer(2)))
            throw new Exception("last element wrong");
    }
}
