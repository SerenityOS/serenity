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

/**
 * @test
 * @bug 8048121
 * @summary javac complex method references: revamp and simplify
 *
 * Make sure that the method reference receiver is evaluated exactly once
 * even in this bridging case.
 */

 public class MethodRefSingleRefEvalBridge {

    interface SAM {
       int m();
    }

    class ZZ {
        // private to force bridging
        private int four() { return 4; }
    }

    static int count = 0;
    ZZ azz = new ZZ();

    static void assertEqual(int expected, int got) {
        if (got != expected)
            throw new AssertionError("Expected " + expected + " got " + got);
    }

    public static void main(String[] args) {
       new MethodRefSingleRefEvalBridge().test();
    }

    ZZ check() {
        count++;
        return azz;
    }

    void test() {
       count = 0;
       SAM s = check()::four;
       assertEqual(1, count);

       count = 0;
       assertEqual(4, s.m());
       assertEqual(0, count);
    }
}
