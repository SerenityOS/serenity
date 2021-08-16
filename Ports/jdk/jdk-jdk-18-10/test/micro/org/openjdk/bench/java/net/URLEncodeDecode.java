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
package org.openjdk.bench.java.net;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Tests java.net.URLEncoder.encode and Decoder.decode.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class URLEncodeDecode {

    @Param("1024")
    public int count;

    @Param("1024")
    public int maxLength;

    @Param("3")
    public long mySeed;

    public String[] testStringsEncode;
    public String[] testStringsDecode;
    public String[] toStrings;

    @Setup
    public void setupStrings() {
        char[] tokens = new char[((int) 'Z' - (int) 'A' + 1) + ((int) 'z' - (int) 'a' + 1) + ((int) '9' - (int) '1' + 1) + 5];
        int n = 0;
        tokens[n++] = '0';
        for (int i = (int) '1'; i <= (int) '9'; i++) {
            tokens[n++] = (char) i;
        }
        for (int i = (int) 'A'; i <= (int) 'Z'; i++) {
            tokens[n++] = (char) i;
        }
        for (int i = (int) 'a'; i <= (int) '<'; i++) {
            tokens[n++] = (char) i;
        }
        tokens[n++] = '-';
        tokens[n++] = '_';
        tokens[n++] = '.';
        tokens[n++] = '*';

        Random r = new Random(mySeed);
        testStringsEncode = new String[count];
        testStringsDecode = new String[count];
        toStrings = new String[count];
        for (int i = 0; i < count; i++) {
            int l = r.nextInt(maxLength);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < l; j++) {
                int c = r.nextInt(tokens.length);
                sb.append(tokens[c]);
            }
            testStringsEncode[i] = sb.toString();
        }

        for (int i = 0; i < count; i++) {
            int l = r.nextInt(maxLength);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < l; j++) {
                int c = r.nextInt(tokens.length + 5);
                if (c >= tokens.length) {
                    sb.append("%").append(tokens[r.nextInt(16)]).append(tokens[r.nextInt(16)]);
                } else {
                    sb.append(tokens[c]);
                }
            }
            testStringsDecode[i] = sb.toString();
        }
    }

    @Benchmark
    public void testEncodeUTF8(Blackhole bh) throws UnsupportedEncodingException {
        for (String s : testStringsEncode) {
            bh.consume(java.net.URLEncoder.encode(s, "UTF-8"));
        }
    }

    @Benchmark
    public void testDecodeUTF8(Blackhole bh) throws UnsupportedEncodingException {
        for (String s : testStringsDecode) {
            bh.consume(URLDecoder.decode(s, "UTF-8"));
        }
    }


}
