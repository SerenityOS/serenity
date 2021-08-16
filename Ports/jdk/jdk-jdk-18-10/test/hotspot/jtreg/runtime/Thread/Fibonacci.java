/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Calculates Fibonacci numbers "recursively" via threads and compares
 *     the result with the classical calculation.
 *     This test is skipped on 32-bit Windows: limited virtual space on Win-32
 *     make this test inherently unstable on Windows with 32-bit VM data model.
 * @requires !(os.family == "windows" & sun.arch.data.model == "32")
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run main/othervm Fibonacci 15
 */

import jdk.test.lib.Asserts;

public class Fibonacci extends Thread {
    private int index;
    private int value;
    private Fibonacci left;
    private Fibonacci right;

    public Fibonacci(int i) {
        index = i;
    }

    private int getValue() {
        return value;
    }

    @Override
    public void run() {
        if (index == 0 || index == 1) {
            // base cases, 0 Fibonacci number = 0, 1 Fibonacci number = 1
            value = index;
        } else {
            // inductive cases
            left = new Fibonacci(index - 2);
            right = new Fibonacci(index - 1);
            left.start();
            right.start();
            try {
                left.join();
                right.join();
            } catch (InterruptedException e) {
                throw new Error("InterruptedException for index " + index, e);
            }
            // compute and terminate
            value = left.getValue() + right.getValue();
        }
    }

    public static int traditionalFibonacci(int n) {
        int n1 = 0, n2 = 1, nn = 0;

        if (n == 0 || n == 1) {
           nn = n;
        }

        for (int i = 1; i < n; ++i) {
            nn = n2 + n1;
            n1 = n2;
            n2 = nn;
        }
        return nn;
    }

    public static void main(String[] args) throws Error,AssertionError {
        int expected;
        int number;
        Fibonacci recursiveFibonacci;

        if (args.length != 1) {
            throw new Error("Error: args.length must be 1");
        }

        number = Integer.parseInt(args[0]);
        recursiveFibonacci = new Fibonacci(number);

        recursiveFibonacci.start();
        try {
            recursiveFibonacci.join();
        } catch (InterruptedException e) {
            throw new Error("InterruptedException in main thread", e);
        }

        expected = traditionalFibonacci(number);

        System.out.println("Fibonacci[" + number + "] = " + expected);

        Asserts.assertEQ(recursiveFibonacci.getValue(), expected,
                          "Unexpected calculated value: " + recursiveFibonacci.getValue() + " expected " + expected );
    }
}
