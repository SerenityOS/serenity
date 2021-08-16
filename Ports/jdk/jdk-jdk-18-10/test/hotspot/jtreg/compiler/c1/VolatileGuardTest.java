/*
 * Copyright (c) 2017, Red Hat Inc. All rights reserved.
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
 * @bug 8175887
 * @summary C1 doesn't respect the JMM with volatile field loads
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:TieredStopAtLevel=1 VolatileGuardTest
 */
public class VolatileGuardTest {
    volatile static private int a;
    static private int b;

    static void test() {
        int tt = b; // makes the JVM CSE the value of b

        while (a == 0) {} // burn
        if (b == 0) {
            System.err.println("wrong value of b");
            System.exit(1); // fail hard to report the error
        }
    }

    public static void main(String [] args) throws Exception {
        for (int i = 0; i < 10; i++) {
            new Thread(VolatileGuardTest::test).start();
        }
        b = 1;
        a = 1;
    }
}
