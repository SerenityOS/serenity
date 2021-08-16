/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @test
 * @bug     4503672
 * @summary AbstractMap.equals and AbstractSet.equals are fragile: they
 *          throw exceptions when they should return false.
 * @author  Josh Bloch
 */
public class Equals {
    public static void main(String[] args) {
        Map m = new HashMap();
        m.put(null, "");
        Map h = new Hashtable();
        h.put("", "");
        if (m.equals(h))
            throw new RuntimeException("1");

        Map m1 = new TreeMap();
        m1.put(new Integer(42), "The Answer");
        Map m2 = new TreeMap();
        m2.put("The Answer", new Integer(42));
        if (m1.equals(m2))
            throw new RuntimeException("3");

        Set s1 = new TreeSet();
        s1.add(new Integer(666));
        Set s2 = new TreeSet();
        s2.add("Great googly moogly!");
        if (s1.equals(s2))
            throw new RuntimeException("2");
    }
}
