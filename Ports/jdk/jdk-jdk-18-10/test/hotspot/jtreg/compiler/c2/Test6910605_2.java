/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6910605
 * @summary C2: NullPointerException/ClassCaseException is thrown when C2 with DeoptimizeALot is used
 *
 * @run main/othervm -Xmx128m -XX:+IgnoreUnrecognizedVMOptions -XX:+DeoptimizeALot
 *      -XX:+DoEscapeAnalysis -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:InlineSmallCode=2000
 *      compiler.c2.Test6910605_2
 */

package compiler.c2;

/*
 * Added InlineSmallCode=2000 to guarantee inlining of StringBuilder::append() to allow scalar replace StringBuilder object.
 *
 * original test: gc/gctests/StringGC
 */

public class Test6910605_2 {
    private final String toAdd = "0123456789abcdef";
    private int maxLength;
    private static final int numberOfThreads = 8;

    private class StringAdder extends Thread {
        private String s;

        public void test() {
            s = s + toAdd;
        }

        public void run() {
            do {
                test();
            } while (s.length() < maxLength);
        }
    }

    public void test() throws InterruptedException {
        maxLength = toAdd.length() * 15000 / numberOfThreads;
        StringAdder[] sa = new StringAdder[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            sa[i] = new StringAdder();
            sa[i].start();
        }
        for (int i = 0; i < numberOfThreads; i++) {
            sa[i].join();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Test6910605_2 t = new Test6910605_2();
        t.test();
    }
}
