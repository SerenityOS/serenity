/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8068430 8069545
 * @summary structural most specific and stuckness
 */

import java.util.HashMap;
import java.util.Map;

public class T8068430 {
    public static void main(String[] args) {
        Map<Integer, String> mp = new HashMap<>();
        mp.put(1, "a");
        mp.put(2, "b");
        mp.put(3, "c");
        mp.put(4, "d");
        System.out.println(mp.entrySet().stream().reduce(0,
                (i, e) -> i + e.getKey(),
                (i1, i2) -> i1 + i2));
    }
}
