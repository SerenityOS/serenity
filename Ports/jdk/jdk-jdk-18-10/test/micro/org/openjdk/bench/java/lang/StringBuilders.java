/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class StringBuilders {

    private String[] strings;
    private String[] str3p4p2;
    private String[] str16p8p7;
    private String[] str3p9p8;
    private String[] str22p40p31;
    private StringBuilder sbLatin1;
    private StringBuilder sbUtf16;

    @Setup
    public void setup() {
        strings = new String[]{"As", "your", "attorney,", "I",
                "advise", "you", "to", "drive", "at", "top", "speed", "it'll",
                "be", "a", "god", "damn", "miracle", "if", "we", "can", "get",
                "there", "before", "you", "turn", "into", "a", "wild", "animal."};
        str3p4p2 = new String[]{"123", "1234", "12"};
        str16p8p7 = new String[]{"1234567890123456", "12345678", "1234567"};
        str3p9p8 = new String[]{"123", "123456789", "12345678"};
        str22p40p31 = new String[]{"1234567890123456789012", "1234567890123456789012345678901234567890", "1234567890123456789012345678901"};
        sbLatin1 = new StringBuilder("Latin1 string");
        sbUtf16 = new StringBuilder("UTF-\uFF11\uFF16 string");
    }

    @Benchmark
    public String concat3p4p2() throws Exception {
        return new StringBuilder(String.valueOf(str3p4p2[0])).append(str3p4p2[1]).append(str3p4p2[2]).toString();
    }

    @Benchmark
    public String concat16p8p7() throws Exception {
        return new StringBuilder(String.valueOf(str16p8p7[0])).append(str16p8p7[1]).append(str16p8p7[2]).toString();
    }

    @Benchmark
    public String concat3p9p8() throws Exception {
        return new StringBuilder(String.valueOf(str3p9p8[0])).append(str3p9p8[1]).append(str3p9p8[2]).toString();
    }

    @Benchmark
    public String concat22p40p31() throws Exception {
        return new StringBuilder(String.valueOf(str22p40p31[0])).append(str22p40p31[1]).append(str22p40p31[2]).toString();
    }

    @Benchmark
    public StringBuilder appendLoop8() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(strings[i]);
        }
        return sb;
    }

    @Benchmark
    public StringBuilder appendLoop16() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(strings[i]);
        }
        return sb;
    }

    @Benchmark
    public String toStringCharWithChar1() {
        StringBuilder result = new StringBuilder();
        result.append('a');
        return result.toString();
    }

    @Benchmark
    public String toStringCharWithChar2() {
        StringBuilder result = new StringBuilder();
        result.append('a');
        result.append('p');
        return result.toString();
    }


    @Benchmark
    public String toStringCharWithChar4() {
        StringBuilder result = new StringBuilder();
        result.append('a');
        result.append('p');
        result.append('a');
        result.append(' ');
        return result.toString();
    }

    @Benchmark
    public String toStringCharWithChar8() {
        StringBuilder result = new StringBuilder();
        result.append('a');
        result.append('p');
        result.append('a');
        result.append(' ');
        result.append('a');
        result.append('p');
        result.append('a');
        result.append(' ');
        return result.toString();
    }

    @Benchmark
    public String toStringCharWithChar16() {
        StringBuilder result = new StringBuilder();
        result.append('a');
        result.append('b');
        result.append('c');
        result.append('d');
        result.append('e');
        result.append('f');
        result.append('g');
        result.append('h');
        result.append('i');
        result.append('j');
        result.append('k');
        result.append('l');
        result.append('m');
        result.append('n');
        result.append('o');
        result.append('p');
        return result.toString();
    }


    @Benchmark
    public String toStringCharWithString8() {
        StringBuilder result = new StringBuilder();
        result.append("a");
        result.append("b");
        result.append("c");
        result.append("d");
        result.append("e");
        result.append("f");
        result.append("g");
        result.append("h");
        return result.toString();
    }


    @Benchmark
    public String toStringCharWithString16() {
        StringBuilder result = new StringBuilder();
        result.append("a");
        result.append("b");
        result.append("c");
        result.append("d");
        result.append("e");
        result.append("f");
        result.append("g");
        result.append("h");
        result.append("i");
        result.append("j");
        result.append("k");
        result.append("l");
        result.append("m");
        result.append("n");
        result.append("o");
        result.append("p");
        return result.toString();
    }


    @Benchmark
    public String toStringCharWithInt8() {
        StringBuilder result = new StringBuilder();
        result.append(2048);
        result.append(31337);
        result.append(0xbeefcace);
        result.append(9000);
        result.append(4711);
        result.append(1337);
        result.append(2100);
        result.append(2600);
        return result.toString();
    }


    @Benchmark
    public String toStringCharWithBool8() {
        StringBuilder result = new StringBuilder();
        result.append(true);
        result.append(false);
        result.append(true);
        result.append(true);
        result.append(false);
        result.append(true);
        result.append(false);
        result.append(false);
        return result.toString();
    }


    @Benchmark
    public String toStringCharWithFloat8() {
        StringBuilder result = new StringBuilder();
        result.append(113.110F);
        result.append(156456.36435637F);
        result.append(65436434.64632F);
        result.append(42654634.64540F);
        result.append(63464351.64537F);
        result.append(634564.645711F);
        result.append(64547.64311F);
        result.append(4763456341.64531F);
        return result.toString();
    }


    @Benchmark
    public String toStringCharWithMixed8() {
        StringBuilder result = new StringBuilder();
        result.append('a');
        result.append("stringelinglinglinglong");
        result.append('a');
        result.append("stringelinglinglinglong");
        result.append('a');
        result.append("stringelinglinglinglong");
        result.append('p');
        result.append("stringelinglinglinglong");
        return result.toString();
    }

    @Benchmark
    public StringBuilder fromLatin1String() {
        return new StringBuilder("Latin1 string");
    }

    @Benchmark
    public StringBuilder fromUtf16String() {
        return new StringBuilder("UTF-\uFF11\uFF16 string");
    }

    @Benchmark
    public StringBuilder fromLatin1StringBuilder() {
        return new StringBuilder(sbLatin1);
    }

    @Benchmark
    public StringBuilder fromUtf16StringBuilder() {
        return new StringBuilder(sbUtf16);
    }

    @Benchmark
    @SuppressWarnings("StringBufferReplaceableByString")
    public String appendSubstring(Data data) {
        String str = data.str;
        int beginIndex = data.beginIndex;
        int endIndex = data.endIndex;

        String substring = str.substring(beginIndex, endIndex);
        return new StringBuilder().append('L').append(substring).append(';').toString();
    }

    @Benchmark
    public String appendBounds(Data data) {
        String str = data.str;
        int beginIndex = data.beginIndex;
        int endIndex = data.endIndex;

        return new StringBuilder().append('L').append(str, beginIndex, endIndex).append(';').toString();
    }

    @Benchmark
    @SuppressWarnings("StringBufferReplaceableByString")
    public String appendSubstringUtf16(Data data) {
        String str = data.utf16Str;
        int beginIndex = data.beginIndex;
        int endIndex = data.endIndex;

        String substring = str.substring(beginIndex, endIndex);

        return new StringBuilder().append('L').append(substring).append(';').toString();
    }

    @Benchmark
    public String appendBoundsUtf16(Data data) {
        String str = data.utf16Str;
        int beginIndex = data.beginIndex;
        int endIndex = data.endIndex;

        return new StringBuilder().append('L').append(str, beginIndex,
                endIndex).append(';').toString();
    }

    @Benchmark
    public String appendBoundsMix(Data data) {
        CharSequence str = data.next();
        int beginIndex = data.beginIndex;
        int endIndex = data.endIndex;

        return new StringBuilder().append('L').append(str, beginIndex,
                endIndex).append(';').toString();
    }

    @State(Scope.Thread)
    public static class Data {
        int i = 0;

        public CharSequence next() {
            i++;
            if (i == 1) {
                return str;
            } else if (i == 2) {
                return utf16Str;
            } else {
                i = 0;
                return cs;
            }
        }

        String str;
        String utf16Str;
        CharSequence cs;

        @Param({"10", "1000"})
        private int length;

        private int beginIndex;
        private int endIndex;

        @Setup
        public void setup() {
            generateData();
            beginIndex = length / 4;
            endIndex = length / 4 * 3;
        }

        private void generateData() {
            char[] chars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                char c = chars[i % chars.length];
                sb.append(c);
            }
            str = sb.toString();
            sb.replace(length / 4 * 2, length / 4 * 2 + 1, "\u04FF");
            utf16Str = sb.toString();
            cs = new StringBuilder(str);
        }
    }
}
