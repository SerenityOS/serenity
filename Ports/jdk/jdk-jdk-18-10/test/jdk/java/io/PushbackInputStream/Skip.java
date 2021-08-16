/*
 * Copyright (c) 1997, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4017196
   @summary Test for correct implementation of PushbackInputStream.skip
 */

import java.io.*;

public class Skip {

    private static void dotest(PushbackInputStream in, int expected)
        throws Exception
    {
        int one, two, last, got;

        in.unread(4);
        in.unread(5);
        in.unread(6);
        in.skip(2);
        if (in.read() != 4) {
            throw new Exception("Failed the n < pushed back bytes case");
        }
        System.err.println("Passed the n < pushed back bytes case");
        one = in.read();
        two = in.read();

        if (two == 7) {
            in.unread(two);
            two = 0;
        }
        in.skip(2);
        last = in.read();
        got = (one + two + last);
        System.err.println("Expected " + expected + " got " + got);
        if (expected != got) {
            throw new Exception("Expected " + expected + " got " + got);
        }
    }


    public static void main(String args[]) throws Exception {
        byte[] data1 = {1, 7, 1, 0, 4};
        byte[] data2 = {1, 1, 1, 1, 5};
        byte[] data3 = {1, 7, 1, 3, 6};
        PushbackInputStream in;

        in = new PushbackInputStream(new ByteArrayInputStream(data1), 3);
        dotest(in, 1);
        in = new PushbackInputStream(new ByteArrayInputStream(data2), 3);
        dotest(in, 7);
        in = new PushbackInputStream(new ByteArrayInputStream(data3), 3);
        dotest(in, 4);
    }

}
