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
 * @bug 4251519 4251520
 * @summary indexOf and lastIndex of used to let you look outside the
 *          valid range in the backing array
 */

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class HeadTailTypeError {
    public static void main(String[] args) throws Exception {
        try {
            SortedMap m = new TreeMap();
            m.headMap(new Object());
            throw new Exception("headMap, natural ordering");
        } catch (ClassCastException e) {
        }

        try {
            SortedMap m = new TreeMap();
            m.tailMap(new Object());
            throw new Exception("tailMap, natural ordering");
        } catch (ClassCastException e) {
        }

        try {
            SortedMap m = new TreeMap(String.CASE_INSENSITIVE_ORDER);
            m.headMap(new Integer(0));
            throw new Exception("headMap, explicit comparator");
        } catch (ClassCastException e) {
        }

        try {
            SortedMap m = new TreeMap(String.CASE_INSENSITIVE_ORDER);
            m.tailMap(new Integer(0));
            throw new Exception("tailMap, explicit comparator");
        } catch (ClassCastException e) {
        }

        try {
            SortedSet m = new TreeSet();
            m.headSet(new Object());
            throw new Exception("headSet, natural ordering");
        } catch (ClassCastException e) {
        }

        try {
            SortedSet m = new TreeSet();
            m.tailSet(new Object());
            throw new Exception("tailSet, natural ordering");
        } catch (ClassCastException e) {
        }

        try {
            SortedSet m = new TreeSet(String.CASE_INSENSITIVE_ORDER);
            m.headSet(new Integer(0));
            throw new Exception("headSet, explicit comparator");
        } catch (ClassCastException e) {
        }

        try {
            SortedSet m = new TreeSet(String.CASE_INSENSITIVE_ORDER);
            m.tailSet(new Integer(0));
            throw new Exception("tailSet, explicit comparator");
        } catch (ClassCastException e) {
        }

        try {
            SortedMap m = new TreeMap();
            m.headMap(null);
            throw new Exception("(null endpoint)headMap, natural ordering");
        } catch (NullPointerException e) {
        }

        try {
            SortedMap m = new TreeMap();
            m.tailMap(null);
            throw new Exception("(null endpoint)tailMap, natural ordering");
        } catch (NullPointerException e) {
        }


        try {
            SortedMap m = new TreeMap(String.CASE_INSENSITIVE_ORDER);
            m.headMap(null);
            throw new Exception("(null endpoint)headMap, explicit comparator");
        } catch (NullPointerException e) {
        }

        try {
            SortedMap m = new TreeMap(String.CASE_INSENSITIVE_ORDER);
            m.tailMap(null);
            throw new Exception("(null endpoint)tailMap, explicit comparator");
        } catch (NullPointerException e) {
        }

        try {
            SortedSet m = new TreeSet();
            m.headSet(null);
            throw new Exception("(null endpoint)headSet, natural ordering");
        } catch (NullPointerException e) {
        }

        try {
            SortedSet m = new TreeSet();
            m.tailSet(null);
            throw new Exception("(null endpoint)tailSet, natural ordering");
        } catch (NullPointerException e) {
        }

        try {
            SortedSet m = new TreeSet(String.CASE_INSENSITIVE_ORDER);
            m.headSet(null);
            throw new Exception("(null endpoint)headSet, explicit comparator");
        } catch (NullPointerException e) {
        }

        try {
            SortedSet m = new TreeSet(String.CASE_INSENSITIVE_ORDER);
            m.tailSet(null);
            throw new Exception("(null endpoint)tailSet, explicit comparator");
        } catch (NullPointerException e) {
        }

        // These should not fail
        SortedMap m = new TreeMap();
        m.headMap(new Integer(0));
        m.tailMap(new Integer(0));
        m = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        m.headMap("llama");
        m.tailMap("llama");

        SortedSet s = new TreeSet();
        s.headSet(new Integer(0));
        s.tailSet(new Integer(0));
        s = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        s.headSet("drama");
        s.tailSet("drama");
    }
}
