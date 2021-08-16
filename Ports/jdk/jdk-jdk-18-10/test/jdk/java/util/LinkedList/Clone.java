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
 * @bug     4216997
 * @summary Cloning a subclass of LinkedList results in an object that isn't
 *          an instance of the subclass.  The same applies to TreeSet and
 *          TreeMap.
 */

import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

public class Clone {
    public static void main(String[] args) {
        LinkedList2 l = new LinkedList2();
        LinkedList2 lClone = (LinkedList2) l.clone();
        if (!(l.equals(lClone) && lClone.equals(l)))
            throw new RuntimeException("LinkedList.clone() is broken 1.");
        l.add("a");
        lClone = (LinkedList2) l.clone();
        if (!(l.equals(lClone) && lClone.equals(l)))
            throw new RuntimeException("LinkedList.clone() is broken 2.");
        l.add("b");
        lClone = (LinkedList2) l.clone();
        if (!(l.equals(lClone) && lClone.equals(l)))
            throw new RuntimeException("LinkedList.clone() is broken 2.");


        TreeSet2 s = new TreeSet2();
        TreeSet2 sClone = (TreeSet2) s.clone();
        if (!(s.equals(sClone) && sClone.equals(s)))
            throw new RuntimeException("TreeSet.clone() is broken.");
        s.add("a");
        sClone = (TreeSet2) s.clone();
        if (!(s.equals(sClone) && sClone.equals(s)))
            throw new RuntimeException("TreeSet.clone() is broken.");
        s.add("b");
        sClone = (TreeSet2) s.clone();
        if (!(s.equals(sClone) && sClone.equals(s)))
            throw new RuntimeException("TreeSet.clone() is broken.");

        TreeMap2 m = new TreeMap2();
        TreeMap2 mClone = (TreeMap2) m.clone();
        if (!(m.equals(mClone) && mClone.equals(m)))
            throw new RuntimeException("TreeMap.clone() is broken.");
        m.put("a", "b");
        mClone = (TreeMap2) m.clone();
        if (!(m.equals(mClone) && mClone.equals(m)))
            throw new RuntimeException("TreeMap.clone() is broken.");
        m.put("c", "d");
        mClone = (TreeMap2) m.clone();
        if (!(m.equals(mClone) && mClone.equals(m)))
            throw new RuntimeException("TreeMap.clone() is broken.");
    }

    private static class LinkedList2 extends LinkedList {}
    private static class TreeSet2    extends TreeSet {}
    private static class TreeMap2    extends TreeMap {}
}
