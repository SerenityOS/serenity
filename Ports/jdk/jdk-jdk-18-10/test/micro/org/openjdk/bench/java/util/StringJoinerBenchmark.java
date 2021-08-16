/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.util;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Trivial benchmark for String joining with {@link java.util.StringJoiner}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(value = 3, jvmArgsAppend = {"-Xms1g", "-Xmx1g"})
public class StringJoinerBenchmark {

    @Benchmark
    public String join(Data data) {
        String[] stringArray = data.stringArray;
        return String.join(",", stringArray);
    }

    @Benchmark
    public String stringJoiner(Data data) {
        String[] stringArray = data.stringArray;
        return Joiner.joinWithStringJoiner(stringArray);
    }

    @State(Scope.Thread)
    public static class Data {

        @Param({"latin", "cyrillic"})
        private String mode;

        @Param({"1", "8", "32", "128"})
        private int length;

        @Param({"5", "20"})
        private int count;

        private String[] stringArray;

        @Setup
        public void setup() {
            stringArray = new String[count];

            for (int i = 0; i < count; i++) {
                String alphabet = getAlphabet(i, mode);
                stringArray[i] = randomString(alphabet, length);
            }
        }

        private String randomString(String alphabet, int length) {
            var tl = ThreadLocalRandom.current();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                sb.append(alphabet.charAt(tl.nextInt(alphabet.length())));
            }
            return sb.toString();
        }

        private static String getAlphabet(int index, String mode) {
            var latin = "abcdefghijklmnopqrstuvwxyz"; //English
            StringBuilder sb = new StringBuilder();
            latin.codePoints().forEach(cp -> sb.append(cp - 'a' + '\u0430'));
            var cyrillic = sb.toString(); // Russian (partial, matching length of latin alphabet)

            String alphabet;
            switch (mode) {
                case "latin" -> alphabet = latin;
                case "cyrillic" -> alphabet = cyrillic;
                default -> throw new RuntimeException("Illegal mode " + mode);
            }
            return alphabet;
        }
    }
}

class Joiner {
    public static String joinWithStringJoiner(String[] stringArray) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (String str : stringArray) {
            joiner.add(str);
        }
        return joiner.toString();
    }
}
