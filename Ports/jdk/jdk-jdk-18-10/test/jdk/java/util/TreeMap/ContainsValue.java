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
 * @bug 4212425
 * @summary TreeMap.containsValue throws NullPointerExc for empty TreeMap
 */

import java.util.Map;
import java.util.TreeMap;

public class ContainsValue {
    public static void main(String[] args) {
        Map map = new TreeMap();

        if (map.containsValue ("gemutlichkeit"))
            throw new RuntimeException("containsValue optimistic (non-null)");

        if (map.containsValue (null))
            throw new RuntimeException("containsValue optimistic (null)");

        map.put("a", null);
        map.put("b", "gemutlichkeit");

        if (!map.containsValue ("gemutlichkeit"))
            throw new RuntimeException("containsValue pessimistic (non-null)");

        if (!map.containsValue (null))
            throw new RuntimeException("containsValue pessimistic (null)");
    }
}
