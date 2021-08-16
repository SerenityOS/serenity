/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.bench.java.util.stream.tasks.PhoneCode;

import org.openjdk.bench.java.util.stream.tasks.DataProviders;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The phone coder problem is trying to find full list of possible
 * mnemonic combination of numbers.
 *
 * The solution is based on Martin Odersky's devoxx 2010 scala talk,
 * where numbers are not allowed in the result, which is not really
 * correct, but we don't care.
 */
public class PhoneCodeProblem {
    // Map Character 'A'-'Z' to digits "2"-"9", key is charCode
    private static final Map<Integer, String> CHAR_CODE;
    // Map a string of digits to a collection of dictionary words
    private static final Map<String, List<String>> WORD_CODES;

    static {
        HashMap<String, String> mnemonics = new HashMap<>(8);
        mnemonics.put("2", "ABC");
        mnemonics.put("3", "DEF");
        mnemonics.put("4", "GHI");
        mnemonics.put("5", "JKL");
        mnemonics.put("6", "MNO");
        mnemonics.put("7", "PQRS");
        mnemonics.put("8", "TUV");
        mnemonics.put("9", "WXYZ");

        CHAR_CODE = new ConcurrentHashMap<>();
        mnemonics.entrySet().stream().forEach(e ->
                e.getValue().chars().forEach(c ->
                    { CHAR_CODE.put(c, e.getKey()); } ));

        WORD_CODES = loadDictionary();
        // System.out.println("Dictionary loaded with " + WORD_CODES.size() + " number entries");
    }

    // Convert a word to its number form
    private static String wordToNumber(String word) {
        return word.chars().mapToObj(CHAR_CODE::get)
                           .reduce("", String::concat);
    }

    // Prepare number -> word lookup table
    private static Map<String, List<String>> loadDictionary() {
        try (Stream<String> s = DataProviders.dictionary()) {
            return s.filter(w -> w.length() > 1)
                    .filter(w -> w.matches("[a-zA-Z]*"))
                    .map(String::toUpperCase)
                    .collect(Collectors.groupingBy(PhoneCodeProblem::wordToNumber));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            return Collections.emptyMap();
        }
    }

    public static Collection<String> wordsForNumber(String number) {
        Collection<String> rv = WORD_CODES.get(number);
        return (null == rv) ? Collections.emptySet() : rv;
    }

    public static Stream<String> get(int length) {
        String digits[] = { "2", "3", "4", "5", "6", "7", "8", "9" };

        Stream<String> s = Arrays.stream(digits);
        for (int i = 1; i < length; i++) {
            s = s.flatMap(d1 -> Arrays.stream(digits).map(d2 -> d1 + d2));
        }
        return s;
    }
}
