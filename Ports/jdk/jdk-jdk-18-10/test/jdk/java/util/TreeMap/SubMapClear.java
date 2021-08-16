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

/*
 * @test
 * @bug     4468802
 * @summary Submap clear tickled a bug in an optimization suggested by
 *          Prof. William Collins (Lafayette College)
 * @author  Josh Bloch
 */

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class SubMapClear {
    public static void main(String[] args) {
        SortedSet treeSet = new TreeSet();
        for (int i = 1; i <=10; i++)
            treeSet.add(new Integer(i));
        Set subSet = treeSet.subSet(new Integer(4),new Integer(10));
        subSet.clear();  // Used to throw exception

        int[] a = { 1, 2, 3, 10 };
        Set s = new TreeSet();
        for (int i = 0; i < a.length; i++)
            s.add(new Integer(a[i]));
        if (!treeSet.equals(s))
            throw new RuntimeException(treeSet.toString());
    }
}
