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
 * @bug 4224271
 * @summary A null Comparator is now specified to indicate natural ordering.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NullComparator {
    public static void main(String[] args) throws Exception {
        List list = new ArrayList(100);
        for (int i=0; i<100; i++)
            list.add(new Integer(i));
        List sorted = new ArrayList(list);
        Collections.shuffle(list);

        Object[] a = list.toArray();
        Arrays.sort(a, null);
        if (!Arrays.asList(a).equals(sorted))
            throw new Exception("Arrays.sort");
        a = list.toArray();
        Arrays.sort(a, 0, 100, null);
        if (!Arrays.asList(a).equals(sorted))
            throw new Exception("Arrays.sort(from, to)");
        if (Arrays.binarySearch(a, new Integer(69)) != 69)
            throw new Exception("Arrays.binarySearch");

        List tmp = new ArrayList(list);
        Collections.sort(tmp, null);
        if (!tmp.equals(sorted))
            throw new Exception("Collections.sort");
        if (Collections.binarySearch(tmp, new Integer(69)) != 69)
            throw new Exception("Collections.binarySearch");
        if (!Collections.min(list, null).equals(new Integer(0)))
            throw new Exception("Collections.min");
        if (!Collections.max(list, null).equals(new Integer(99)))
            throw new Exception("Collections.max");
    }
}
