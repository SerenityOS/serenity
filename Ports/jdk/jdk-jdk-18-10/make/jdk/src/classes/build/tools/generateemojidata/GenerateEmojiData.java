/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package build.tools.generateemojidata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generate EmojiData.java
 *    args[0]: Full path string to the template file
 *    args[1]: Full path string to the directory that contains "emoji-data.txt"
 *    args[2]: Full path string to the generated .java file
 */
public class GenerateEmojiData {
    public static void main(String[] args) {
        try {
            final Range[] last = new Range[1]; // last extended pictographic range
            last[0] = new Range(0, 0);

            List<Range> extPictRanges = Files.lines(Paths.get(args[1], "emoji", "emoji-data.txt"))
                .filter(Predicate.not(l -> l.startsWith("#") || l.isBlank()))
                .filter(l -> l.contains("; Extended_Pictograph"))
                .map(l -> new Range(l.replaceFirst(" .*", "")))
                .sorted()
                .collect(ArrayList<Range>::new,
                    (list, r) -> {
                        // collapsing consecutive pictographic ranges
                        int lastIndex = list.size() - 1;
                        if (lastIndex >= 0) {
                            Range lastRange = list.get(lastIndex);
                            if (lastRange.last + 1 == r.start) {
                                list.set(lastIndex, new Range(lastRange.start, r.last));
                                return;
                            }
                        }
                        list.add(r);
                    },
                    ArrayList<Range>::addAll);


            // make the code point conditions
            // only very few codepoints below 0x2000 are "emojis", so separate them
            // out to generate a fast-path check that can be efficiently inlined
            String lowExtPictCodePoints = extPictRanges.stream()
                    .takeWhile(r -> r.last < 0x2000)
                    .map(r -> rangeToString(r))
                    .collect(Collectors.joining(" ||\n", "", ";\n"));

            String highExtPictCodePoints = extPictRanges.stream()
                    .dropWhile(r -> r.last < 0x2000)
                    .map(r -> rangeToString(r))
                    .collect(Collectors.joining(" ||\n", "", ";\n"));

            // Generate EmojiData.java file
            Files.write(Paths.get(args[2]),
                Files.lines(Paths.get(args[0]))
                    .flatMap(l -> {
                        if (l.equals("%%%EXTPICT_LOW%%%")) {
                            return Stream.of(lowExtPictCodePoints);
                        } else if (l.equals("%%%EXTPICT_HIGH%%%")) {
                            return Stream.of(highExtPictCodePoints);
                        } else {
                            return Stream.of(l);
                        }
                    })
                    .collect(Collectors.toList()),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String rangeToString(Range r) {
        if (r.start == r.last) {
            return (" ".repeat(16) + "cp == 0x" + toHexString(r.start));
        } else  if (r.start == r.last - 1) {
            return " ".repeat(16) + "cp == 0x" + toHexString(r.start) + " ||\n" +
                    " ".repeat(16) + "cp == 0x" + toHexString(r.last);
        } else {
            return " ".repeat(15) + "(cp >= 0x" + toHexString(r.start) +
                    " && cp <= 0x" + toHexString(r.last) + ")";
        }
    }

    static int toInt(String hexStr) {
        return Integer.parseUnsignedInt(hexStr, 16);
    }

    static String toHexString(int cp) {
        String ret = Integer.toUnsignedString(cp, 16).toUpperCase();
        if (ret.length() < 4) {
            ret = "0".repeat(4 - ret.length()) + ret;
        }
        return ret;
    }

    static class Range implements Comparable<Range> {
        int start;
        int last;

        Range (int start, int last) {
            this.start = start;
            this.last = last;
        }

        Range (String input) {
            input = input.replaceFirst("\\s#.*", "");
            start = toInt(input.replaceFirst("[\\s\\.].*", ""));
            last = input.contains("..") ?
                    toInt(input.replaceFirst(".*\\.\\.", "")
                            .replaceFirst(";.*", "").trim())
                    : start;
        }

        @Override
        public String toString() {
            return "Start: " + toHexString(start) + ", Last: " + toHexString(last);
        }

        @Override
        public int compareTo(Range other) {
            return Integer.compare(start, other.start);
        }
    }
}
