/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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




/*
 * @test
 * @bug 8159244
 * @summary Verifies that no partially initialized String object escapes from
 *          C2's String concat optimization in a highly concurrent setting.
 *          This test triggers the bug in about 1 out of 10 runs.
 * @requires vm.gc == "Parallel" | vm.gc == "null"
 *
 * @compile -XDstringConcat=inline TestStringObjectInitialization.java
 * @run main/othervm/timeout=300 -XX:+IgnoreUnrecognizedVMOptions -XX:-UseCompressedOops -XX:-CompactStrings
 *                               -XX:-UseG1GC -XX:+UseParallelGC
 *                               compiler.stringopts.TestStringObjectInitialization
 */

package compiler.stringopts;

import java.util.Arrays;

public class TestStringObjectInitialization {

    String myString;

    public static void main(String[] args) throws Exception {
        TestStringObjectInitialization t = new TestStringObjectInitialization();
        // Create some threads that concurrently update 'myString'
        for (int i = 0; i < 100; ++i) {
            (new Thread(new Runner(t))).start();
        }
        Thread last = new Thread(new Runner(t));
        last.start();
        last.join();
    }

    private void add(String message) {
        // String escapes to other threads here
        myString += message;
    }

    public void run(String s, String[] sArray) {
        // Trigger C2's string concatenation optimization
        add(s + Arrays.toString(sArray) + " const ");
    }

    public void reset() {
        // Reset string to avoid OOMEs
        myString = "";
    }

    private static class Runner implements Runnable {
        private TestStringObjectInitialization test;

        public Runner(TestStringObjectInitialization t) {
            test = t;
        }

        public void run() {
            String[] array = {"a", "b", "c"};
            for (int i = 0; i < 100_000; ++i) {
                test.run("a", array);
                test.reset();
            }
        }
    }
}

