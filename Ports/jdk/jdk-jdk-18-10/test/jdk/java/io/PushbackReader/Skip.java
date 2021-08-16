/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4398210
 * @summary check skip method after pushing data back
 *
 */

import java.io.*;

public class Skip {

    public static void main(String args[]) throws Exception {
        test1();
    }

    private static void test1() throws Exception {
        char[] buf = new char[20];
        for (int i=0; i<20; i++)
            buf[i] = (char)i;
        CharArrayReader car = new CharArrayReader(buf);
        PushbackReader pr = new PushbackReader(car, 10);
        check(pr.read(), 0);
        // Check skip without unread chars present
        pr.skip(1);
        check(pr.read(), 2);
        pr.unread(2);
        pr.unread(1);
        // Check skip over and beyond unread chars
        pr.skip(4);
        check(pr.read(), 5);
        check(pr.read(), 6);
        pr.unread(6);
        pr.unread(5);
        // Check skip within unread chars
        pr.skip(1);
        check(pr.read(), 6);
        check(pr.read(), 7);
        // Check skip after unread chars have been used
        pr.skip(3);
        check(pr.read(), 11);
    }

    private static void check (int i, int j) {
        if (i != j)
            throw new RuntimeException("Test failed");
    }
}
