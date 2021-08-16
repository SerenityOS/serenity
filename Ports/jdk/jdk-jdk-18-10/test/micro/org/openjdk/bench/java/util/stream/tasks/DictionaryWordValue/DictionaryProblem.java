/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.util.stream.tasks.DictionaryWordValue;

import org.openjdk.bench.java.util.stream.tasks.DataProviders;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DictionaryProblem {

    private static final int DICTIONARY_REPEAT_RATE = 40;

    private static final String[] dict;

    static {
        int size;
        int idx = 0;
        List<String> d = Collections.emptyList();
        try (Stream<String> s = DataProviders.dictionary()) {
            d = s.collect(Collectors.<String>toList());
        } catch (Exception e) {
            // ignore
        }
        size = d.size() * DICTIONARY_REPEAT_RATE;
        dict = new String[size];
        for (int i = 0; i < DICTIONARY_REPEAT_RATE; i++) {
            d.sort(new IdxComparator(i));
            for (String s : d) {
                // copy the whole string
                dict[idx++] = new String(s.toCharArray());
            }
        }
        assert (idx == dict.length);
    }

    public static String[] get() {
        return dict;
    }

    /**
     * A word value is the sum of alphabet value of each characters in a word.
     *
     * @param word The word
     * @return The word value
     */
    public static int wordValue(String word) {
        char[] ar = word.toLowerCase().toCharArray();
        int value = 0;
        for (char c: ar) {
            int v = c - 'a' + 1;
            if (v < 1 || v > 26) {
                // skip non-alphabet
                continue;
            }
            value += (c - 'a' + 1);
        }
        return value;
    }

    static class IdxComparator implements Comparator<String> {
        private final int index;

        public IdxComparator(int i) {
            index = i;
        }

        @Override
        public int compare(String a, String b) {
            if (a.length() > index && b.length() > index) {
                return (a.charAt(index) - b.charAt(index));
            } else {
                return (a.length() - b.length());
            }
        }
    }

}
