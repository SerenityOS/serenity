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
 * @bug 4268780
 * @summary Collection-views of submap-views of synchronized-views of
 *          SortedMap objects do not synchronize on the correct object.
 *          (Got that?)
 */

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class ViewSynch {
    static final Integer ZERO = new Integer(0);
    static final Int INT_ZERO = new Int(0);
    static final Int INT_ONE = new Int(1);
    static SortedMap m = Collections.synchronizedSortedMap(new TreeMap());
    static Map m2 = m.tailMap(ZERO);
    static Collection c = m2.values();

    public static void main(String[] args) {
        for (int i=0; i<10000; i++)
            m.put(new Integer(i), INT_ZERO);

        new Thread() {
            public void run() {
                for (int i=0; i<100; i++) {
                    Thread.yield();
                    m.remove(ZERO);
                    m.put(ZERO, INT_ZERO);
                }
            }
        }.start();

        c.contains(INT_ONE);
    }
}

/**
 * Like Integer, except yields while doing equals comparison, to allow
 * for interleaving.
 */
class Int {
    Integer x;
    Int(int i) {x = new Integer(i);}

    public boolean equals(Object o) {
        Thread.yield();
        Int i = (Int)o;
        return x.equals(i.x);
    }

    public int hashCode() {return x.hashCode();}
}
