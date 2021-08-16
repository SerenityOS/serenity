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
   @bug 1238944
   @summary Check for correct implementation of LineNumberInputStream.available
   */

import java.io.*;


public class Available {

    static void check(int a, int bound) throws Exception {
        if (a > bound) {
            throw new Exception("Available returned " + a + " > " + bound);
        }
    }

    public static void main(String args[]) throws Exception {
        LineNumberInputStream in = new LineNumberInputStream(new MyInStream());
        check(in.available(), 5);
        in.read();
        in.read();
        check(in.available(), 4);
        in.read();
        in.read();
        in.read();
        check(in.available(), 2);
    }

}


class MyInStream extends InputStream {

    char[] buf = {'a', 'b', 'c', 'd', '\n',
                  'e', 'f', '\r', '\n', 'g'};
    int ctr = 0;

    public int read() {
        return ((ctr == 12) ? -1 : (int)buf[ctr++]);
    }

    public int available() {
        return (10 - ctr);
    }

}
