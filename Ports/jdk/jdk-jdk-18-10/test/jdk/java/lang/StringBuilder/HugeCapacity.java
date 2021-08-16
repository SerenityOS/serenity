/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8149330 8218227
 * @summary Capacity should not get close to Integer.MAX_VALUE unless
 *          necessary
 * @requires (sun.arch.data.model == "64" & os.maxMemory >= 6G)
 * @run main/othervm -Xms5G -Xmx5G -XX:+CompactStrings HugeCapacity true
 * @run main/othervm -Xms5G -Xmx5G -XX:-CompactStrings HugeCapacity false
 */

public class HugeCapacity {
    private static int failures = 0;

    public static void main(String[] args) {
        if (args.length == 0) {
           throw new IllegalArgumentException("Need the argument");
        }
        boolean isCompact = Boolean.parseBoolean(args[0]);

        testLatin1(isCompact);
        testUtf16();
        testHugeInitialString();
        testHugeInitialCharSequence();
        if (failures > 0) {
            throw new RuntimeException(failures + " tests failed");
        }
    }

    private static void testLatin1(boolean isCompact) {
        try {
            int divisor = isCompact ? 2 : 4;
            StringBuilder sb = new StringBuilder();
            sb.ensureCapacity(Integer.MAX_VALUE / divisor);
            sb.ensureCapacity(Integer.MAX_VALUE / divisor + 1);
        } catch (OutOfMemoryError oom) {
            oom.printStackTrace();
            failures++;
        }
    }

    private static void testUtf16() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append('\u042b');
            sb.ensureCapacity(Integer.MAX_VALUE / 4);
            sb.ensureCapacity(Integer.MAX_VALUE / 4 + 1);
        } catch (OutOfMemoryError oom) {
            oom.printStackTrace();
            failures++;
        }
    }

    private static void testHugeInitialString() {
        try {
            String str = "Z".repeat(Integer.MAX_VALUE - 8);
            StringBuilder sb = new StringBuilder(str);
        } catch (OutOfMemoryError ignore) {
        } catch (Throwable unexpected) {
            unexpected.printStackTrace();
            failures++;
        }
    }

    private static void testHugeInitialCharSequence() {
        try {
            CharSequence seq = new MyHugeCharSeq();
            StringBuilder sb = new StringBuilder(seq);
        } catch (OutOfMemoryError ignore) {
        } catch (Throwable unexpected) {
            unexpected.printStackTrace();
            failures++;
        }
    }

    private static class MyHugeCharSeq implements CharSequence {
        public char charAt(int i) {
            throw new UnsupportedOperationException();
        }
        public int length() { return Integer.MAX_VALUE; }
        public CharSequence subSequence(int st, int e) {
            throw new UnsupportedOperationException();
        }
        public String toString() { return ""; }
    }
}
