/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

class GetAvailableLocales {

    public static void main(String[] args) {
        Set<String> expected = Set.of(args);
        Set<String> actual =
            Arrays.stream(Locale.getAvailableLocales())
                  // "(root)" for Locale.ROOT rather than ""
                  .map(loc -> loc.equals(Locale.ROOT) ? "(root)" : loc.toString())
                  .collect(Collectors.toSet());

        if (!expected.equals(actual)) {
            diff(expected, actual);
            System.exit(1);
        }
    }

    private static void diff(Set<String> expected, Set<String> actual) {
        Set<String> s1 = new TreeSet<>(expected);
        s1.removeAll(actual);
        if (!s1.isEmpty()) {
            System.out.println("\tMissing locale(s): " + s1);
        }
        Set<String> s2 = new TreeSet<>(actual);
        s2.removeAll(expected);
        if (!s2.isEmpty()) {
            System.out.println("\tExtra locale(s): " + s2);
        }
    }
}
