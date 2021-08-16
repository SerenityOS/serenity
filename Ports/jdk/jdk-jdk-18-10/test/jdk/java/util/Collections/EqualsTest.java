/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7144488
 * @summary Infinite recursion for some equals tests in Collections
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EqualsTest {
    public static void main(String[] args) {
        boolean test;

        /* synchronizedList test */
        List list = Collections.synchronizedList(new ArrayList());
        list.add(list);
        test = list.equals(list);
        assertTrue(test);
        list.remove(list);

        /* synchronizedSet test */
        Set s = Collections.synchronizedSet(new HashSet());
        s.add(s);
        test = s.equals(s);
        assertTrue(test);

        /* synchronizedMap test */
        Map m =  Collections.synchronizedMap(new HashMap());
        test = m.equals(m);
        assertTrue(test);

    }

    private static void assertTrue(boolean b) {
        if (!b)
            throw new RuntimeException("assertion failed");
    }
}

