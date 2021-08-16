/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6310858
 * @summary Tests for toArray
 * @author  Martin Buchholz
 */

import java.util.*;

public class ToArray {
    enum Country { FRENCH, POLISH }
    public static void main(String[] args) throws Throwable {
        Map<Country, String> m = new EnumMap<Country, String>(Country.class);
        m.put(Country.FRENCH, "connection");
        m.put(Country.POLISH, "sausage");

        Object[] z = m.entrySet().toArray();
        System.out.println(Arrays.toString(z));
        if (! (z.getClass() == Object[].class &&
               z.length == 2 &&
               ((Map.Entry)z[0]).getKey() == Country.FRENCH &&
               ((Map.Entry)z[1]).getKey() == Country.POLISH))
            throw new AssertionError();

        Map.Entry[] x1 = new Map.Entry[3];
        x1[2] = m.entrySet().iterator().next();
        Map.Entry[] x2 = m.entrySet().toArray(x1);
        System.out.println(Arrays.toString(x2));
        if (! (x1 == x2 &&
               x2[0].getKey() == Country.FRENCH &&
               x2[1].getKey() == Country.POLISH &&
               x2[2] == null))
            throw new AssertionError();

        Map.Entry[] y1 = new Map.Entry[1];
        Map.Entry[] y2 = m.entrySet().toArray(y1);
        System.out.println(Arrays.toString(y2));
        if (! (y1 != y2 &&
               y2.length == 2 &&
               y2[0].getKey() == Country.FRENCH &&
               y2[1].getKey() == Country.POLISH))
            throw new AssertionError();
    }
}
