/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4944561
 * @summary Test hashCode() to have less than 10% of hash code conflicts.
 * @modules jdk.localedata
 */

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HashCodeTest {

    public static void main(String[] args) {
        Locale[] locales = Locale.getAvailableLocales();
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        Map<Integer, Locale> map = new HashMap<>(locales.length);
        int conflicts = 0;

        for (int i = 0; i < locales.length; i++) {
            Locale loc = locales[i];
            int hc = loc.hashCode();
            min = Math.min(hc, min);
            max = Math.max(hc, max);
            Integer key = hc;
            if (map.containsKey(key)) {
                conflicts++;
                System.out.println("conflict: " + map.get(key) + ", " + loc);
            } else {
                map.put(key, loc);
            }
        }
        System.out.println(locales.length + " locales: conflicts=" + conflicts
                + ", min=" + min + ", max=" + max + ", diff=" + (max - min));
        if (conflicts >= (locales.length / 10)) {
            throw new RuntimeException("too many conflicts: " + conflicts
                    + " per " + locales.length + " locales");
        }
    }
}
