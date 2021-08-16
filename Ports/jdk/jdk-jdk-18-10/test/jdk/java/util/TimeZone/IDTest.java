/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4509255 5055567 6176318 7090844
 * @summary Tests consistencies of time zone IDs.
 */

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

public class IDTest {
    public static void main(String[] args) {
        Set<String> ids = new HashSet<>();
        Map<Integer, Set<String>> tree = new TreeMap<>();

        String[] tzs = TimeZone.getAvailableIDs();
        String[] tzs2 = TimeZone.getAvailableIDs();
        if (tzs.length != tzs2.length) {
            throw new RuntimeException("tzs.length(" + tzs.length
                                       + ") != tzs2.length(" + tzs2.length + ")");
        }
        for (int i = 0; i < tzs.length; i++) {
            if (tzs[i] != tzs2[i]) {
                throw new RuntimeException(i + ": " + tzs[i] + " != " + tzs2[i]);
            }
        }

        System.out.println("Total: " + tzs.length + " time zone IDs");
        for (String id : tzs) {
            ids.add(id);
            TimeZone tz = TimeZone.getTimeZone(id);
            Integer offset = tz.getRawOffset();
            Set<String> s = tree.get(offset);
            if (s == null) {
                s = new HashSet<>();
                tree.put(offset, s);
            }
            s.add(id);
        }

        for (Integer key : tree.keySet()) {
            Set<String> s1 = tree.get(key);

            // Make sure no duplicates in the other sets
            for (Integer k : tree.keySet()) {
                if (k.equals(key)) {
                    continue;
                }
                Set<String> s2 = new HashSet<>(tree.get(k));
                s2.retainAll(s1);
                if (!s2.isEmpty()) {
                    throw new RuntimeException("s1 included in the subset for " + (k.intValue()/60000) +
                                               " (" + s2 + " shouldn't be in s1)");
                }
            }

            // Check the getAvailableIDs(int) call to return the same
            // set of IDs
            int offset = key.intValue();
            tzs = TimeZone.getAvailableIDs(offset);
            tzs2 = TimeZone.getAvailableIDs(offset);
            if (!Arrays.equals(tzs, tzs2)) {
                throw new RuntimeException("inconsistent tzs from getAvailableIDs("+offset+")");
            }
            Set<String> s2 = new HashSet<>();
            s2.addAll(Arrays.asList(tzs));
            if (!s1.equals(s2)) {
                throw new RuntimeException("s1 != s2 for " + offset/60000 +
                                           " (diff=" + getDiff(s1, s2) + ")");
            }
            if (!ids.containsAll(s2)) {
                throw new RuntimeException("s2 isn't a subset of ids (" + getDiff(s2, ids) +
                                           " not in ids)");
            }
        }

        for (Integer key : tree.keySet()) {
            Set<String> s1 = tree.get(key);
            ids.removeAll(s1);
        }
        if (!ids.isEmpty()) {
            throw new RuntimeException("ids didn't become empty. (" + ids + ")");
        }
    }

    private static String getDiff(Set<String> set1, Set<String> set2) {
        Set<String> s1 = new HashSet<>(set1);
        s1.removeAll(set2);

        Set<String> s2 = new HashSet<>(set2);
        s2.removeAll(set1);
        s2.addAll(s1);
        return s2.toString();
    }
}
