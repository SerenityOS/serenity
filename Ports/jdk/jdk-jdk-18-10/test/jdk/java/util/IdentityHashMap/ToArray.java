/*
 * Copyright (c) 2001, 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4485486 6197726 6232484
 * @summary IdentityHashMap's entrySet toArray tests
 * @author  Josh Bloch, Martin Buchholz
 */

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ToArray {
    public static void main(String[] args) {
        //----------------------------------------------------------------
        // new ArrayList(IdentityHashMap.entrySet())
        // used to return bogus entries.
        //----------------------------------------------------------------
        Map<String,String> mm = new IdentityHashMap<>();
        mm.put("foo", "bar");
        mm.put("baz", "quux");
        List<Map.Entry<String,String>> lm = new ArrayList<>(mm.entrySet());
        String s = lm.toString();
        if (! (s.equals("[foo=bar, baz=quux]") ||
               s.equals("[baz=quux, foo=bar]")))
            throw new Error("bad string");

        //----------------------------------------------------------------
        // IdentityHashMap's lack of internal entry objects caused
        // the inherited (AbstractMap) version of toArray to
        // return garbage when called on the entrySet view.
        //----------------------------------------------------------------
        Map m = new IdentityHashMap();
        m.put("french", "connection");
        m.put("polish", "sausage");
        Object[] mArray = m.entrySet().toArray();
        if (mArray[0] == mArray[1])
            throw new RuntimeException("Broken");

        mArray[0].toString();
        mArray[1].toString();

        //----------------------------------------------------------------
        // IdentityHashMap.entrySet().toArray(T[] a) used to simply
        // return toArray() !
        //----------------------------------------------------------------
        IdentityHashMap<Integer,Integer> map = new IdentityHashMap<>();
        Set<Map.Entry<Integer,Integer>> es = map.entrySet();
        if (es.toArray().length != 0)
            throw new Error("non-empty");
        if (es.toArray(new Object[]{Boolean.TRUE})[0] != null)
            throw new Error("non-null");
        map.put(7,49);
        if (es.toArray().length != 1)
            throw new Error("length");
        Object[] x = es.toArray(new Object[]{Boolean.TRUE, Boolean.TRUE});
        if (x[1] != null)
            throw new Error("non-null");
        Map.Entry e = (Map.Entry) x[0];
        if (! e.getKey().equals(new Integer(7)))
            throw new Error("bad key");
        if (! e.getValue().equals(new Integer(49)))
            throw new Error("bad value");
    }
}
