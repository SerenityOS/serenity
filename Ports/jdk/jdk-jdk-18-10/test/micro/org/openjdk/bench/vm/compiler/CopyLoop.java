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
package org.openjdk.bench.vm.compiler;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark measuring effect of loop optimizations.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class CopyLoop {

    private MyString s;
    private Buf b;

    @Setup
    public void setup() {
        s = new MyString("foobarba");
        b = new Buf();
    }

    /**
     * Basic implementation, as a Java programmer would write it.
     */
    @Benchmark
    public void testCharAtLoop() throws Exception {
        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            b.byteBuf[i] = (byte) c;
        }
    }

    /** Inline charAt and remove the redundant bounds checks. */
    @Benchmark
    public void testInlineCharAtLoop() throws Exception {
        for (int i = 0; i < s.count; i++) {
            int c = s.value[i + s.offset];
            b.byteBuf[i] = (byte) c;
        }
    }

    /**
     * Unroll the loop (I cheat here because I know that the String will always be an even length, real implementation
     * would require a pre-loop).
     */
    @Benchmark
    public void testInlineAndUnrollCharAtLoop() throws Exception {
        int startI = 0;
        if ((s.count & 0xFFFE) != s.count) {
            int c = s.value[s.offset];
            b.byteBuf[0] = (byte) c;
            startI = 1;
        }
        for (int i = startI; i < s.count - 1; i += 2) {
            int c = s.value[i + s.offset];
            int d = s.value[i + s.offset + 1];
            b.byteBuf[i] = (byte) c;
            b.byteBuf[i + 1] = (byte) d;
        }
    }

    /** Hoist computation of constants outside of the loop. */
    @Benchmark
    public void testInlineAndUnrollAndHoistCharAtLoop() throws Exception {
        byte[] byteBuf = b.byteBuf;
        if (byteBuf == null) {
            throw new NullPointerException();
        }
        char[] value = s.value;
        int offset = s.offset;

        int startI = offset;

        if ((s.count & 0xFFFE) != s.count) {
            int c = value[offset];
            byteBuf[0] = (byte) c;
            startI++;
        }

        int maxI = s.count + offset - 1;
        for (int i = startI; i < maxI; i += 2) {
            int c = value[i];
            int d = value[i + 1];
            byteBuf[i] = (byte) c;
            byteBuf[i + 1] = (byte) d;
        }
    }

    private static class Buf {
        private byte[] byteBuf = new byte[100];
    }

    public static final class MyString {
        private char value[];

        private int offset;

        private int count;

        public MyString(String original) {
            value = original.toCharArray();
            offset = 0;
            count = value.length;
        }

        public int length() {
            return count;
        }

        public char charAt(int index) {
            Objects.checkIndex(index, count);
            return value[index + offset];
        }

    }

}
